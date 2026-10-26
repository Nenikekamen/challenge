package com.challenge.transaction.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageServiceTest {

    @Test
    void shouldResolveMessagesFromPropertiesUsingSpringMessageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:messages-eng");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);

        MessageService messageService = new MessageService(messageSource);

        assertEquals("Transaction error", messageService.getMessage("transaction.error.title"));
        assertEquals("The transaction information is required",
                messageService.getMessage("transaction.validator.request.required"));
        assertEquals("The amount must be greater than 1.00 MXN",
                messageService.getMessage("transaction.validator.amount.min", "1.00"));
        assertEquals("DEBIT transactions cannot exceed 10000.00 MXN per operation",
                messageService.getMessage("transaction.validator.debit.max-amount", "10000.00"));
        assertEquals("The currency USD is not supported, only MXN is accepted",
                messageService.getMessage("transaction.validator.currency.unsupported", "USD", "MXN"));
        assertEquals("The page parameter cannot be negative",
                messageService.getMessage("pagination.page.negative"));
        assertEquals("The limit parameter must be between 1 and 100",
                messageService.getMessage("pagination.limit.invalid"));
        assertEquals("This transaction was already sent, please wait before sending the same transaction again",
                messageService.getMessage("transaction.idempotency.conflict"));
        assertEquals("The provider returned an unknown status: INVALID",
                MessageService.get("provider.status.unknown", "INVALID"));
    }

    @Test
    void shouldFallbackToResourceBundleWhenSpringSourceIsNull() {
        MessageService messageService = new MessageService(null);
        String message = messageService.getMessage("transaction.error.title");
        assertEquals("Transaction error", message);
    }
}
