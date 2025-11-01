package com.bank.frontend.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.ObjectError;

/**
 * Resolves localized messages using the current request locale.
 */
@Service
@RequiredArgsConstructor
public class LocalizationService {

    private final MessageSource messageSource;

    /**
     * Resolves the message for the given code and optional arguments.
     *
     * @param code message key
     * @param args optional interpolation arguments
     * @return localized message
     */
    public String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, locale);
    }

    /**
     * Resolves the message for the given code and optional arguments. Returns a fallback message
     * when the key is missing.
     *
     * @param code message key
     * @param defaultMessage fallback string when key is absent
     * @param args optional interpolation arguments
     * @return localized message or fallback
     */
    public String getMessageOrDefault(String code, String defaultMessage, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    /**
     * Resolves the message represented by the given {@link ObjectError}, falling back to the
     * default message when no translation is available.
     *
     * @param error validation error to resolve
     * @return localized validation message
     */
    public String resolveMessage(ObjectError error) {
        if (error == null) {
            return "";
        }
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(error, locale);
        } catch (NoSuchMessageException ex) {
            return error.getDefaultMessage();
        }
    }

    /**
     * Resolves the message represented by the given {@link MessageSourceResolvable}, falling back
     * to the provided default when a translation cannot be found.
     *
     * @param resolvable message source resolvable
     * @param defaultMessage fallback text when key is missing
     * @return localized message or fallback
     */
    public String resolveMessageOrDefault(MessageSourceResolvable resolvable, String defaultMessage) {
        if (resolvable == null) {
            return defaultMessage;
        }
        Locale locale = LocaleContextHolder.getLocale();
        try {
            return messageSource.getMessage(resolvable, locale);
        } catch (NoSuchMessageException ex) {
            return defaultMessage;
        }
    }
}
