package com.challenge.transaction.aplication.service.impl;

import com.challenge.transaction.aplication.service.RetrieveTransactionService;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.adapter.input.rest.mapper.TransactionMapper;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.database.repository.TransactionRepository;
import com.challenge.transaction.infrastructure.config.MessageService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RetrieveTransactionServiceImpl implements RetrieveTransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final MessageService messageService;

    public RetrieveTransactionServiceImpl(TransactionRepository transactionRepository,
                                          TransactionMapper transactionMapper,
                                          MessageService messageService) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.messageService = messageService;
    }

    @Override
    public List<TransactionResponse> retrieveTransactions(String accountId, Status status, TransactionType type,
                                                          int page, int limit) {
        if (page < 0) {
            throw new TransactionException(messageService.getMessage("pagination.page.negative"), HttpStatus.BAD_REQUEST.value());
        }
        if (limit <= 0 || limit > 100) {
            throw new TransactionException(messageService.getMessage("pagination.limit.invalid"),
                    HttpStatus.BAD_REQUEST.value());
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<TransactionEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (accountId != null && !accountId.isBlank()) {
                predicates.add(cb.equal(root.get("accountId"), accountId));
            }
            if (status != null) {
                com.challenge.transaction.domain.model.Status domainStatus =
                        com.challenge.transaction.domain.model.Status.valueOf(status.name());
                predicates.add(cb.equal(root.get("status"), domainStatus));
            }
            if (type != null) {
                com.challenge.transaction.domain.model.TransactionType domainType =
                        com.challenge.transaction.domain.model.TransactionType.valueOf(type.name());
                predicates.add(cb.equal(root.get("type"), domainType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<TransactionEntity> entities = transactionRepository.findAll(spec, pageable).getContent();

        return entities.stream()
                .map(transactionMapper::toDomain)
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }
}
