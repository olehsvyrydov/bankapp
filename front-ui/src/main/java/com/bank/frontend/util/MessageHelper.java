package com.bank.frontend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class MessageHelper {

    private static final Logger log = LoggerFactory.getLogger(MessageHelper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> CANDIDATE_KEYS = List.of("message", "error", "detail", "description", "reason");

    // Map backend error messages to localization keys
    private static final Map<String, String> ERROR_MESSAGE_MAPPING = Map.of(
        "Cannot delete bank account with non-zero balance", "bankAccount.delete.nonZeroBalance",
        "Insufficient balance", "bankAccount.insufficientBalance",
        "Insufficient funds", "bankAccount.insufficientBalance"
    );

    public static String pickUserMessage(String defaultMessage, String friendlyMessage, String rawMessage) {
        if (friendlyMessage != null && !friendlyMessage.isBlank() && !friendlyMessage.equals(defaultMessage)) {
            return friendlyMessage;
        }

        String parsed = extractReadable(rawMessage);
        if (parsed != null && !parsed.isBlank()) {
            return parsed;
        }

        return defaultMessage;
    }

    /**
     * Extracts a localization key for the given backend error message.
     * Returns null if no mapping exists.
     */
    public static String getLocalizationKey(String backendMessage) {
        if (backendMessage == null || backendMessage.isBlank()) {
            return null;
        }
        return ERROR_MESSAGE_MAPPING.get(backendMessage.trim());
    }

    public static String extractReadable(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }
        String trimmed = rawMessage.trim();

        // Try to parse JSON response from backend
        if (trimmed.contains("{") && trimmed.contains("}")) {
            int jsonStart = trimmed.indexOf('{');
            int jsonEnd = trimmed.lastIndexOf('}') + 1;
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonPart = trimmed.substring(jsonStart, jsonEnd);
                try {
                    JsonNode node = OBJECT_MAPPER.readTree(jsonPart);
                    if (node.isObject()) {
                        for (String key : CANDIDATE_KEYS) {
                            JsonNode value = node.get(key);
                            if (value != null) {
                                String text = value.asText(null);
                                if (text != null && !text.isBlank() && !text.equals("Business Error")) {
                                    return text;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Unable to parse raw message as JSON: {}", jsonPart, ex);
                }
            }
        }

        // If no JSON found or couldn't parse, check if it's a simple user-friendly message
        // Return the raw message if it looks user-friendly (short and doesn't contain technical details)
        if (!containsTechnicalDetails(trimmed) && trimmed.length() < 200) {
            return trimmed;
        }

        return null;
    }

    /**
     * Checks if the message contains technical details that should be filtered.
     */
    private static boolean containsTechnicalDetails(String message) {
        return message.contains("Exception") ||
               message.contains("stacktrace") ||
               message.contains("at ") ||
               message.contains(".java:") ||
               message.contains("HTTP") ||
               message.contains("status") ||
               message.contains("timestamp") ||
               message.contains("[") && message.contains("]");
    }
}
