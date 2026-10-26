package com.challenge.transaction.infrastructure.adapter.output.provider;

import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class TransactionProviderRestAdapterTest {

    @Mock
    private MessageService messageService;

    private MockRestServiceServer mockServer;
    private TransactionProviderRestAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8081");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        adapter = new TransactionProviderRestAdapter(restClient, "/provider/v1/execute", messageService);
    }

    @Test
    @DisplayName("Should execute request successfully and parse approved provider response")
    void execute_success_returnsApprovedResponse() {
        String jsonResponse = """
                {
                    "status": "APPROVED",
                    "transactionId": "prov-tx-001",
                    "providerReference": "ref-001",
                    "balance": 1500.50,
                    "executedAt": "2026-10-25T12:00:00Z"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8081/provider/v1/execute"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ProviderExecuteRequest request = new ProviderExecuteRequest("acc-1", "CREDIT", new BigDecimal("500.00"), "MXN");

        ProviderExecuteResponse response = adapter.execute(request);

        assertNotNull(response);
        assertEquals("APPROVED", response.status());
        assertEquals("prov-tx-001", response.transactionId());
        assertEquals("ref-001", response.providerReference());
        assertEquals(new BigDecimal("1500.50"), response.balance());
        assertEquals(Instant.parse("2026-10-25T12:00:00Z"), response.executedAt());

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle 4xx/5xx provider response and return parsed error response payload")
    void execute_providerReturnsClientErrorWithPayload_returnsParsedResponse() {
        String errorPayload = """
                {
                    "status": "REJECTED",
                    "code": "INSUFFICIENT_FUNDS",
                    "message": "Account has not enough balance",
                    "executedAt": "2026-10-25T12:00:00Z"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8081/provider/v1/execute"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorPayload));

        ProviderExecuteRequest request = new ProviderExecuteRequest("acc-1", "DEBIT", new BigDecimal("500.00"), "MXN");

        ProviderExecuteResponse response = adapter.execute(request);

        assertNotNull(response);
        assertEquals("REJECTED", response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.code());
        assertEquals("Account has not enough balance", response.message());

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw TransactionException when provider returns empty body")
    void execute_emptyResponse_throwsException() {
        when(messageService.getMessage("provider.response.empty")).thenReturn("The provider returned an empty response");

        mockServer.expect(requestTo("http://localhost:8081/provider/v1/execute"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ProviderExecuteRequest request = new ProviderExecuteRequest("acc-1", "CREDIT", new BigDecimal("100.00"), "MXN");

        TransactionException exception = assertThrows(TransactionException.class, () -> adapter.execute(request));
        assertEquals(HttpStatus.BAD_GATEWAY.value(), exception.getStatusCode());
        assertEquals("The provider returned an empty response", exception.getMessage());

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw TransactionException when network/service error occurs")
    void execute_connectionError_throwsException() {
        when(messageService.getMessage("provider.service.unavailable")).thenReturn("The provider service is not available");

        // Use a RestClient pointing to an invalid port or simulate failure
        RestClient restClient = RestClient.builder().baseUrl("http://invalid-host-that-fails.invalid:9999").build();
        TransactionProviderRestAdapter badAdapter = new TransactionProviderRestAdapter(restClient, "/provider/v1/execute", messageService);

        ProviderExecuteRequest request = new ProviderExecuteRequest("acc-1", "CREDIT", new BigDecimal("100.00"), "MXN");

        TransactionException exception = assertThrows(TransactionException.class, () -> badAdapter.execute(request));
        assertEquals(HttpStatus.BAD_GATEWAY.value(), exception.getStatusCode());
        assertEquals("The provider service is not available", exception.getMessage());
    }
}
