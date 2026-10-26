package com.challenge.transaction.infrastructure.adapter.input.rest;

import com.challenge.transaction.aplication.service.AddTransactionService;
import com.challenge.transaction.aplication.service.RetrieveTransactionService;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.Status;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionRequest;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionResponse;
import com.challenge.transaction.infrastructure.adapter.input.rest.dto.TransactionType;
import com.challenge.transaction.infrastructure.config.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Transactions", description = "Transaction related operations")
public class TransactionController {

    private final AddTransactionService addTransactionService;
    private final RetrieveTransactionService retrieveTransactionService;
    private final MessageService messageService;

    public TransactionController(AddTransactionService addTransactionService,
                                 RetrieveTransactionService retrieveTransactionService,
                                 MessageService messageService) {
        this.addTransactionService = addTransactionService;
        this.retrieveTransactionService = retrieveTransactionService;
        this.messageService = messageService;
    }

    @Operation(summary = "Create a transaction", description = "Registers a new transaction and returns its result")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate transaction",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestBody TransactionRequest request) {
        try {
            TransactionResponse response = addTransactionService.addTransaction(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionException(messageService.getMessage("transaction.process.error"), e,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Operation(summary = "List transactions",
            description = "Retrieves transactions filtered by accountId, status and type, with pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal error", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(description = "Account identifier")
            @RequestParam(required = false) String accountId,
            @Parameter(description = "Transaction status")
            @RequestParam(required = false) Status status,
            @Parameter(description = "Transaction type")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Page number (zero-based)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of records per page")
            @RequestParam(required = false, defaultValue = "20") int limit) {
        try {
            List<TransactionResponse> transactions =
                    retrieveTransactionService.retrieveTransactions(accountId, status, type, page, limit);
            return ResponseEntity.ok(transactions);
        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            throw new TransactionException(messageService.getMessage("transaction.retrieve.error"), e,
                    HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
