package com.challenge.transaction.infrastructure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ProviderRestClientConfig {

    @Bean
    public RestClient providerRestClient(@Value("${provider.base-url:http://localhost:8081}") String baseUrl,
                                         @Value("${provider.connect-timeout:2s}") Duration connectTimeout,
                                         @Value("${provider.read-timeout:5s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
