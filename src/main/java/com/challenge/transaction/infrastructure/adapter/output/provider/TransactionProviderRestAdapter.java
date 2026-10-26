package com.challenge.transaction.infrastructure.adapter.output.provider;

import com.challenge.transaction.aplication.port.output.TransactionProviderPort;
import com.challenge.transaction.domain.exception.TransactionException;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteRequest;
import com.challenge.transaction.infrastructure.adapter.output.provider.dto.ProviderExecuteResponse;
import com.challenge.transaction.infrastructure.config.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TransactionProviderRestAdapter implements TransactionProviderPort {

    private final RestClient restClient;
    private final String executePath;
    private final MessageService messageService;

    public TransactionProviderRestAdapter(RestClient providerRestClient,
                                          @Value("${provider.execute-path:/provider/v1/execute}") String executePath,
                                          MessageService messageService) {
        this.restClient = providerRestClient;
        this.executePath = executePath;
        this.messageService = messageService;
    }

    @Override
    public ProviderExecuteResponse execute(ProviderExecuteRequest request) {
        try {
            ProviderExecuteResponse response = restClient.post()
                    .uri(executePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (req, res) -> {
                                // the provider returns the rejection payload in the body, it is handled as a result
                            })
                    .body(ProviderExecuteResponse.class);
            if (response == null) {
                throw new TransactionException(messageService.getMessage("provider.response.empty"),
                        HttpStatus.BAD_GATEWAY.value());
            }
            return response;
        } catch (RestClientException e) {
            throw new TransactionException(messageService.getMessage("provider.service.unavailable"), e,
                    HttpStatus.BAD_GATEWAY.value());
        }
    }
}
