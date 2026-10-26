package com.challenge.transaction.infrastructure.config;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

@Component
public class MessageService {

    private static MessageSource staticMessageSource;
    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
        staticMessageSource = messageSource;
    }

    public String getMessage(String key, Object... args) {
        return get(messageSource, key, args);
    }

    public static String get(String key, Object... args) {
        return get(staticMessageSource, key, args);
    }

    private static String get(MessageSource source, String key, Object... args) {
        if (source != null) {
            try {
                return source.getMessage(key, args, Locale.ENGLISH);
            } catch (NoSuchMessageException ignored) {
            }
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages-eng", Locale.ROOT);
            String pattern = bundle.getString(key);
            if (args != null && args.length > 0) {
                return MessageFormat.format(pattern, args);
            }
            return pattern;
        } catch (Exception e) {
            return key;
        }
    }
}
