package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.nio.charset.StandardCharsets;

/**
 * Custom STOMP Message Converter to ensure proper JSON serialization
 * and fix frame parsing issues with Flutter clients
 */
@Component
public class StompMessageConverter extends AbstractMessageConverter {

    private final ObjectMapper objectMapper;

    public StompMessageConverter() {
        super(new MimeType("application", "json", StandardCharsets.UTF_8));
        this.objectMapper = new ObjectMapper();

        // Configure ObjectMapper for optimal JSON output
        this.objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                false);

        // Register JSR310 module for Java 8 time support
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // Support all classes for JSON conversion
        return true;
    }

    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
        Object payload = message.getPayload();

        if (payload instanceof byte[]) {
            try {
                String json = new String((byte[]) payload, StandardCharsets.UTF_8);
                System.out
                        .println("📥 STOMP JSON Received: " + json.substring(0, Math.min(100, json.length())) + "...");
                return objectMapper.readValue(json, targetClass);
            } catch (Exception e) {
                System.err.println("❌ Error parsing JSON from STOMP frame: " + e.getMessage());
                throw new MessageConversionException("Failed to convert JSON message", e);
            }
        }

        return payload;
    }

    @Override
    protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            System.out.println("📤 STOMP JSON Sending: " + json.substring(0, Math.min(100, json.length())) + "...");
            System.out.println("📤 STOMP Frame Size: " + jsonBytes.length + " bytes");
            System.out.println("📤 STOMP JSON Full Length: " + json.length() + " characters");
            System.out.println("📤 STOMP UTF-8 Byte Length: " + jsonBytes.length + " bytes");

            // Log the exact JSON for debugging
            System.out.println("📤 STOMP Full JSON: " + json);

            // Ensure proper UTF-8 encoding for Flutter compatibility
            return jsonBytes;

        } catch (Exception e) {
            System.err.println("❌ Error converting object to JSON: " + e.getMessage());
            throw new MessageConversionException("Failed to convert object to JSON", e);
        }
    }
}
