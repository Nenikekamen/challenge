package com.challenge.transaction.infrastructure.adapter.input.rest.mapper;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.domain.model.Status;
import com.challenge.transaction.domain.model.Transaction;
import com.challenge.transaction.domain.model.TransactionType;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionMapperTest {

    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TransactionMapper.class);
    }

    @Test
    @DisplayName("Should map TransactionRequest to ProviderExecuteRequest")
    void toProviderRequest_mapsCorrectly() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType.CREDIT,
                new BigDecimal("250.75"),
                "MXN",
                "Monthly salary"
        );

        ProviderExecuteRequest providerRequest = mapper.toProviderRequest(request);

        assertNotNull(providerRequest);
        assertEquals("acc-123", providerRequest.accountId());
        assertEquals("CREDIT", providerRequest.type());
        assertEquals(new BigDecimal("250.75"), providerRequest.amount());
        assertEquals("MXN", providerRequest.currency());
    }

    @Test
    @DisplayName("Should return null when mapping null TransactionRequest to ProviderExecuteRequest")
    void toProviderRequest_nullRequest_returnsNull() {
        assertNull(mapper.toProviderRequest(null));
    }

    @Test
    @DisplayName("Should map Request, ProviderExecuteResponse and id to domain Transaction")
    void toDomain_fromRequestAndProviderResponse_mapsCorrectly() {
        TransactionRequest request = new TransactionRequest(
                "acc-123",
                com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType.CREDIT,
                new BigDecimal("150.00"),
                "MXN",
                "Payment"
        );
        Instant now = Instant.parse("2026-10-25T12:00:00Z");
        ProviderExecuteResponse providerResponse = new ProviderExecuteResponse(
                "ref-123",
                "tx-provider-1",
                "APPROVED",
                new BigDecimal("1150.00"),
                now,
                null,
                null
        );

        Transaction domain = mapper.toDomain(request, providerResponse, "custom-id-1");

        assertNotNull(domain);
        assertEquals("custom-id-1", domain.id());
        assertEquals("acc-123", domain.accountId());
        assertEquals(TransactionType.CREDIT, domain.type());
        assertEquals(new BigDecimal("150.00"), domain.amount());
        assertEquals("MXN", domain.currency());
        assertEquals("Payment", domain.description());
        assertEquals(Status.APPROVED, domain.status());
        assertEquals("ref-123", domain.providerReference());
        assertEquals("tx-provider-1", domain.providerTransactionId());
        assertEquals(new BigDecimal("1150.00"), domain.balanceAfter());
        assertNull(domain.failureCode());
        assertNull(domain.failureMessage());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), domain.createdAt());
    }

    @Test
    @DisplayName("Should return null when mapping null inputs in toDomain")
    void toDomain_nullInputs_returnsExpected() {
        Transaction domain = mapper.toDomain(null, null, null);
        assertNull(domain);
    }

    @Test
    @DisplayName("Should map domain Transaction to TransactionEntity")
    void toEntity_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime nowLdt = LocalDateTime.of(2026, 10, 25, 12, 0, 0);
        Transaction domain = new Transaction(
                id.toString(),
                "acc-123",
                TransactionType.DEBIT,
                new BigDecimal("50.00"),
                "MXN",
                "Coffee",
                Status.APPROVED,
                "ref-456",
                "prov-tx-789",
                new BigDecimal("950.00"),
                null,
                null,
                nowLdt
        );

        TransactionEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("acc-123", entity.getAccountId());
        assertEquals(TransactionType.DEBIT, entity.getType());
        assertEquals(new BigDecimal("50.00"), entity.getAmount());
        assertEquals("MXN", entity.getCurrency());
        assertEquals("Coffee", entity.getDescription());
        assertEquals(Status.APPROVED, entity.getStatus());
        assertEquals("ref-456", entity.getProviderReference());
        assertEquals("prov-tx-789", entity.getProviderTransactionId());
        assertEquals(new BigDecimal("950.00"), entity.getBalanceAfter());
        assertEquals(nowLdt.toInstant(ZoneOffset.UTC), entity.getCreatedAt());
        assertEquals(nowLdt.toInstant(ZoneOffset.UTC), entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Should return null when mapping null Transaction to Entity")
    void toEntity_null_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    @DisplayName("Should map TransactionEntity to domain Transaction")
    void toDomain_fromEntity_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-25T10:30:00Z");
        TransactionEntity entity = new TransactionEntity();
        entity.setId(id);
        entity.setAccountId("acc-123");
        entity.setType(TransactionType.CREDIT);
        entity.setAmount(new BigDecimal("200.00"));
        entity.setCurrency("MXN");
        entity.setDescription("Top-up");
        entity.setStatus(Status.APPROVED);
        entity.setProviderReference("ref-999");
        entity.setProviderTransactionId("prov-999");
        entity.setBalanceAfter(new BigDecimal("1200.00"));
        entity.setIdempotencyKey("hash-123");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        Transaction domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(id.toString(), domain.id());
        assertEquals("acc-123", domain.accountId());
        assertEquals(TransactionType.CREDIT, domain.type());
        assertEquals(new BigDecimal("200.00"), domain.amount());
        assertEquals("MXN", domain.currency());
        assertEquals("Top-up", domain.description());
        assertEquals(Status.APPROVED, domain.status());
        assertEquals("ref-999", domain.providerReference());
        assertEquals("prov-999", domain.providerTransactionId());
        assertEquals(new BigDecimal("1200.00"), domain.balanceAfter());
        assertEquals(LocalDateTime.ofInstant(now, ZoneOffset.UTC), domain.createdAt());
    }

    @Test
    @DisplayName("Should return null when mapping null Entity to Domain")
    void toDomain_nullEntity_returnsNull() {
        assertNull(mapper.toDomain((TransactionEntity) null));
    }

    @Test
    @DisplayName("Should map domain Transaction to TransactionResponse")
    void toResponse_mapsCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0, 0);
        Transaction domain = new Transaction(
                "tx-1",
                "acc-123",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Bonus",
                Status.APPROVED,
                "ref-1",
                "prov-1",
                new BigDecimal("2500.00"),
                null,
                null,
                now
        );

        TransactionResponse response = mapper.toResponse(domain);

        assertNotNull(response);
        assertEquals("tx-1", response.id());
        assertEquals("acc-123", response.accountId());
        assertEquals(com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType.CREDIT, response.type());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertEquals("MXN", response.currency());
        assertEquals("Bonus", response.description());
        assertEquals(com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status.APPROVED, response.status());
        assertEquals("ref-1", response.providerReference());
        assertEquals(new BigDecimal("2500.00"), response.balanceAfter());
        assertEquals(now, response.createdAt());
    }

    @Test
    @DisplayName("Should map list of domain Transactions to list of TransactionResponses")
    void toResponses_mapsListCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0, 0);
        Transaction t1 = new Transaction("tx-1", "acc-1", TransactionType.CREDIT, new BigDecimal("10.00"), "MXN", null, Status.APPROVED, null, null, null, null, null, now);
        Transaction t2 = new Transaction("tx-2", "acc-2", TransactionType.DEBIT, new BigDecimal("20.00"), "MXN", null, Status.REJECTED, null, null, null, null, null, now);

        List<TransactionResponse> responses = mapper.toResponses(List.of(t1, t2));

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("tx-1", responses.get(0).id());
        assertEquals("tx-2", responses.get(1).id());
    }

    @Test
    @DisplayName("Should return null or empty when mapping null list to responses")
    void toResponses_nullList_returnsNull() {
        assertNull(mapper.toResponses(null));
    }

    @Test
    @DisplayName("Should convert status string correctly in default helper method")
    void toStatus_tests() {
        assertEquals(Status.PENDING, mapper.toStatus(null));
        assertEquals(Status.PENDING, mapper.toStatus(""));
        assertEquals(Status.PENDING, mapper.toStatus("   "));
        assertEquals(Status.APPROVED, mapper.toStatus("APPROVED"));
        assertEquals(Status.APPROVED, mapper.toStatus("approved"));
        assertEquals(Status.REJECTED, mapper.toStatus("rejected"));

        TransactionException exception = assertThrows(TransactionException.class, () -> mapper.toStatus("INVALID_STATUS"));
        assertEquals(HttpStatus.BAD_GATEWAY.value(), exception.getStatusCode());
    }

    @Test
    @DisplayName("Should convert Status enum to String")
    void toProviderStatus_tests() {
        assertEquals("APPROVED", mapper.toProviderStatus(Status.APPROVED));
        assertEquals("REJECTED", mapper.toProviderStatus(Status.REJECTED));
        assertNull(mapper.toProviderStatus(null));
    }

    @Test
    @DisplayName("Should convert between UUID and String")
    void uuidString_tests() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, mapper.toUuid(uuid.toString()));
        assertNull(mapper.toUuid(null));

        assertEquals(uuid.toString(), mapper.toStringId(uuid));
        assertNull(mapper.toStringId(null));
    }

    @Test
    @DisplayName("Should convert between Instant and LocalDateTime")
    void dateConversions_tests() {
        LocalDateTime ldt = LocalDateTime.of(2026, 10, 25, 12, 0, 0);
        Instant instant = ldt.toInstant(ZoneOffset.UTC);

        assertEquals(ldt, mapper.toLocalDateTime(instant));
        assertEquals(instant, mapper.toInstant(ldt));
        assertNull(mapper.toInstant(null));

        assertNotNull(mapper.toLocalDateTime(null));
    }
}
