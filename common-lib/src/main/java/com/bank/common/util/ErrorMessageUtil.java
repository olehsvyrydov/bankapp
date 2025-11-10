package com.bank.common.util;
import lombok.experimental.UtilityClass;
/**
 * Utility class for sanitizing error messages to be user-friendly.
 * Filters out technical details, stack traces, JSON errors, and other sensitive information.
 */
@UtilityClass
public class ErrorMessageUtil {
    private static final int MAX_MESSAGE_LENGTH = 200;
    /**
     * Extracts user-friendly error message from technical error messages.
     * Filters out JSON, stack traces, and technical details.
     *
     * @param message        the technical error message
     * @param defaultMessage the default user-friendly message to return if filtering is needed
     * @return user-friendly error message
     */
    public static String extractUserFriendlyMessage(String message, String defaultMessage) {
        if (message == null || message.isEmpty()) {
            return defaultMessage;
        }
        // Check if message contains JSON or technical details
        if (containsTechnicalDetails(message)) {
            return defaultMessage;
        }
        // Check for common user-friendly messages
        if (isUserFriendlyMessage(message)) {
            return message;
        }
        return defaultMessage;
    }
    /**
     * Checks if the message contains technical details that should be filtered.
     *
     * @param message the message to check
     * @return true if message contains technical details
     */
    private static boolean containsTechnicalDetails(String message) {
        return message.contains("{") ||
               message.contains("[") ||
               message.contains("Exception") ||
               message.contains("Error:") ||
               message.contains("status") ||
               message.contains("timestamp") ||
               message.contains("stacktrace") ||
               message.contains("at ") ||
               message.contains(".java:") ||
               message.length() > MAX_MESSAGE_LENGTH;
    }
    /**
     * Checks if the message is user-friendly and can be displayed directly.
     *
     * @param message the message to check
     * @return true if message is user-friendly
     */
    private static boolean isUserFriendlyMessage(String message) {
        String lowerMessage = message.toLowerCase();

        // Common user-friendly patterns
        if (lowerMessage.contains("already exists") ||
            lowerMessage.contains("not found") ||
            lowerMessage.contains("username") ||
            lowerMessage.contains("email") ||
            lowerMessage.contains("password") ||
            lowerMessage.contains("invalid") ||
            lowerMessage.contains("required") ||
            lowerMessage.contains("expired") ||
            lowerMessage.contains("denied")) {
            return true;
        }

        // Banking-specific user-friendly messages
        return lowerMessage.contains("insufficient funds") ||
            lowerMessage.contains("insufficient balance") ||
            lowerMessage.contains("account") ||
            lowerMessage.contains("currency") ||
            lowerMessage.contains("transfer") ||
            lowerMessage.contains("blocked") ||
            lowerMessage.contains("suspicious") ||
            lowerMessage.contains("suspended") ||
            lowerMessage.contains("limit exceeded") ||
            lowerMessage.contains("balance");
    }
    /**
     * Sanitizes an error message for logging purposes.
     * Removes sensitive information but keeps technical details for debugging.
     *
     * @param message the message to sanitize
     * @return sanitized message suitable for logging
     */
    public static String sanitizeForLogging(String message) {
        if (message == null) {
            return "null";
        }
        // Remove potential sensitive data patterns
        String sanitized = message.replaceAll("password[\"']?\\s*[:=]\\s*[\"']?[^\\s,}\"']+", "password=***");
        sanitized = sanitized.replaceAll("token[\"']?\\s*[:=]\\s*[\"']?[^\\s,}\"']+", "token=***");
        sanitized = sanitized.replaceAll("secret[\"']?\\s*[:=]\\s*[\"']?[^\\s,}\"']+", "secret=***");
        return sanitized;
    }
}
