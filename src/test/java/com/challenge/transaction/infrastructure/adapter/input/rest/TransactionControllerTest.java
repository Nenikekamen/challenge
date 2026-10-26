package com.challenge.transaction.infrastructure.adapter.input.rest;

import com.challenge.transaction.aplication.service.AddTransactionService;
import com.challenge.transaction.aplication.service.RetrieveTransactionService;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.adapter.input.rest.handler.TransactionExceptionHandler;
import com.challenge.transaction.infrastructure.config.MessageService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private AddTransactionService addTransactionService;

    @Mock
    private RetrieveTransactionService retrieveTransactionService;

    @Mock
    private MessageService messageService;

    private TransactionController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(addTransactionService, retrieveTransactionService, messageService);
        TransactionExceptionHandler exceptionHandler = new TransactionExceptionHandler(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("POST /v1/transactions - should create transaction and return 201 Created")
    void createTransaction_success_returnsCreated() throws Exception {
        TransactionResponse response = new TransactionResponse(
                "tx-123",
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Salary",
                Status.APPROVED,
                "ref-1",
                "prov-1",
                new BigDecimal("1500.00"),
                null,
                null,
                LocalDateTime.of(2026, 10, 25, 12, 0, 0)
        );

        when(addTransactionService.addTransaction(any(TransactionRequest.class))).thenReturn(response);

        String jsonRequest = """
                {
                    "accountId": "acc-1",
                    "type": "CREDIT",
                    "amount": 500.00,
                    "currency": "MXN",
                    "description": "Salary"
                }
                """;

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tx-123"))
                .andExpect(jsonPath("$.accountId").value("acc-1"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /v1/transactions - direct method call returns 201 Created")
    void createTransaction_directCall_returnsCreatedResponse() {
        TransactionRequest request = new TransactionRequest(
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Salary"
        );
        TransactionResponse expectedResponse = new TransactionResponse(
                "tx-123",
                "acc-1",
                TransactionType.CREDIT,
                new BigDecimal("500.00"),
                "MXN",
                "Salary",
                Status.APPROVED,
                "ref-1",
                "prov-1",
                new BigDecimal("1500.00"),
                null,
                null,
                LocalDateTime.now()
        );

        when(addTransactionService.addTransaction(request)).thenReturn(expectedResponse);

        ResponseEntity<TransactionResponse> responseEntity = controller.createTransaction(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    @Test
    @DisplayName("POST /v1/transactions - should rethrow TransactionException")
    void createTransaction_throwsTransactionException() {
        TransactionRequest request = new TransactionRequest("acc-1", TransactionType.CREDIT, new BigDecimal("0.50"), "MXN", "Deposit");
        when(addTransactionService.addTransaction(request))
                .thenThrow(new TransactionException("Amount too small", HttpStatus.BAD_REQUEST.value()));

        TransactionException exception = assertThrows(TransactionException.class, () -> controller.createTransaction(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("Amount too small", exception.getMessage());
    }

    @Test
    @DisplayName("POST /v1/transactions - should wrap unexpected exception in 500 TransactionException")
    void createTransaction_unexpectedException_wrapsIn500() {
        when(messageService.getMessage("transaction.process.error")).thenReturn("Error while processing the transaction");
        TransactionRequest request = new TransactionRequest("acc-1", TransactionType.CREDIT, new BigDecimal("100.00"), "MXN", "Deposit");
        when(addTransactionService.addTransaction(request)).thenThrow(new RuntimeException("Database timeout"));

        TransactionException exception = assertThrows(TransactionException.class, () -> controller.createTransaction(request));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals("Error while processing the transaction", exception.getMessage());
    }

    @Test
    @DisplayName("GET /v1/transactions - should retrieve transactions and return 200 OK")
    void getTransactions_success_returnsOk() throws Exception {
        TransactionResponse r1 = new TransactionResponse(
                "tx-1",
                "acc-2",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Payment",
                Status.APPROVED,
                "ref-1",
                "prov-1",
                new BigDecimal("1100.00"),
                null,
                null,
                LocalDateTime.of(2026, 10, 25, 12, 0, 0)
        );

        when(retrieveTransactionService.retrieveTransactions("acc-2", Status.APPROVED, TransactionType.CREDIT, 0, 20))
                .thenReturn(List.of(r1));

        mockMvc.perform(get("/v1/transactions")
                        .param("accountId", "acc-2")
                        .param("status", "APPROVED")
                        .param("type", "CREDIT")
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("tx-1"))
                .andExpect(jsonPath("$[0].accountId").value("acc-2"));

        verify(retrieveTransactionService).retrieveTransactions("acc-2", Status.APPROVED, TransactionType.CREDIT, 0, 20);
    }

    @Test
    @DisplayName("GET /v1/transactions - direct method call returns OK list")
    void getTransactions_directCall_returnsList() {
        TransactionResponse r1 = new TransactionResponse(
                "tx-1",
                "acc-2",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "MXN",
                "Test",
                Status.APPROVED,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );
        when(retrieveTransactionService.retrieveTransactions(eq("acc-2"), any(), any(), eq(0), eq(10)))
                .thenReturn(List.of(r1));

        ResponseEntity<List<TransactionResponse>> responseEntity = controller.getTransactions("acc-2", null, null, 0, 10);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(1, responseEntity.getBody().size());
    }

    @Test
    @DisplayName("GET /v1/transactions - should rethrow TransactionException")
    void getTransactions_throwsTransactionException() {
        when(retrieveTransactionService.retrieveTransactions(null, null, null, -1, 20))
                .thenThrow(new TransactionException("Page negative", HttpStatus.BAD_REQUEST.value()));

        TransactionException exception = assertThrows(TransactionException.class,
                () -> controller.getTransactions(null, null, null, -1, 20));
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("Page negative", exception.getMessage());
    }

    @Test
    @DisplayName("GET /v1/transactions - should wrap unexpected exception in 500 TransactionException")
    void getTransactions_unexpectedException_wrapsIn500() {
        when(messageService.getMessage("transaction.retrieve.error")).thenReturn("Error while retrieving the transactions");
        when(retrieveTransactionService.retrieveTransactions(null, null, null, 0, 20))
                .thenThrow(new RuntimeException("Connection failed"));

        TransactionException exception = assertThrows(TransactionException.class,
                () -> controller.getTransactions(null, null, null, 0, 20));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals("Error while retrieving the transactions", exception.getMessage());
    }
}
