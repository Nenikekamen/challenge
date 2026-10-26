package com.challenge.transaction.aplication.service.impl;

import com.challenge.transaction.aplication.port.output.TransactionProviderPort;
import com.challenge.transaction.aplication.service.AddTransactionService;
import com.challenge.transaction.aplication.validator.TransactionValidator;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.domain.model.Transaction;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.mapper.TransactionMapper;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.database.repository.TransactionRepository;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddTransactionServiceImpl implements AddTransactionService {

    private final TransactionProviderPort transactionProviderPort;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final List<TransactionValidator> transactionValidators;
    private final MessageService messageService;

    public AddTransactionServiceImpl(TransactionProviderPort transactionProviderPort,
                                     TransactionRepository transactionRepository,
                                     TransactionMapper transactionMapper,
                                     List<TransactionValidator> transactionValidators,
                                     MessageService messageService) {
        this.transactionProviderPort = transactionProviderPort;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.transactionValidators = transactionValidators;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public TransactionResponse addTransaction(TransactionRequest request) {
        if (request == null) {
            throw new TransactionException(messageService.getMessage("transaction.validator.request.required"), HttpStatus.BAD_REQUEST.value());
        }

        String key = generateIdempotencyKey(request);

        // 1. Validar primero si ya existe la clave de idempotencia en BD antes de llamar al proveedor
        Optional<TransactionEntity> existing = transactionRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            throw new TransactionException(
                    messageService.getMessage("transaction.idempotency.conflict"),
                    HttpStatus.CONFLICT.value());
        }

        // 2. Si no existe, validar reglas de negocio
        transactionValidators.forEach(validator -> validator.validate(request));

        // 3. Enviar la petición al servicio del proveedor externo
        ProviderExecuteRequest providerRequest = transactionMapper.toProviderRequest(request);
        ProviderExecuteResponse providerResponse;
        try {
            providerResponse = transactionProviderPort.execute(providerRequest);
        } catch (TransactionException e) {
            providerResponse = new ProviderExecuteResponse(
                    null,
                    null,
                    "REJECTED",
                    null,
                    Instant.now(),
                    String.valueOf(e.getStatusCode()),
                    e.getMessage()
            );
        } catch (Exception e) {
            providerResponse = new ProviderExecuteResponse(
                    null,
                    null,
                    "REJECTED",
                    null,
                    Instant.now(),
                    "PROVIDER_ERROR",
                    e.getMessage() != null ? e.getMessage() : messageService.getMessage("provider.error.connection")
            );
        }

        // 4. Mapear y registrar en la BD con su clave de idempotencia
        Transaction transaction = transactionMapper.toDomain(request, providerResponse,
                UUID.randomUUID().toString());

        TransactionEntity entity = transactionMapper.toEntity(transaction);
        entity.setIdempotencyKey(key);

        try {
            TransactionEntity saved = transactionRepository.saveAndFlush(entity);
            return transactionMapper.toResponse(transactionMapper.toDomain(saved));
        } catch (DataIntegrityViolationException e) {
            // En caso de carrera, la segunda inserción viola la restricción única: reportar conflicto
            throw new TransactionException(
                    messageService.getMessage("transaction.idempotency.conflict"),
                    e,
                    HttpStatus.CONFLICT.value());
        }
    }

    private String generateIdempotencyKey(TransactionRequest request) {
        // Tiempo de 10 minutos: todas las solicitudes dentro de la misma ventana comparten clave
        long epochMillis = Instant.now().toEpochMilli();
        long intervalPetition = epochMillis / (10L * 60L * 1000L);

        String canonical = String.join("|",
                safe(request.accountId()),
                safe(String.valueOf(request.type())),
                safe(request.amount() == null ? null : request.amount().toPlainString()),
                safe(request.currency()),
                safe(request.description()),
                String.valueOf(intervalPetition)
        );

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return toHex(digest); // 64 chars, cabe en VARCHAR(100)
        } catch (NoSuchAlgorithmException e) {
            // Fallback improbable: usa UUID si SHA-256 no está disponible
            return UUID.randomUUID().toString();
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
