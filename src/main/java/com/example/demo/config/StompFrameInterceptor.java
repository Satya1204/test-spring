package com.example.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP Frame Interceptor to fix frame parsing issues
 * Ensures proper content-length headers and frame assembly
 */
@Component
public class StompFrameInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        System.out
                .println("🔍 STOMP Interceptor - preSend called, accessor: " + (accessor != null ? "present" : "null"));

        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            System.out.println("🔍 STOMP Command: " + command);

            // Handle outbound messages (server to client)
            if (command == StompCommand.MESSAGE) {
                System.out.println("🔍 Processing MESSAGE frame...");

                // Ensure content-length header is set for proper frame parsing
                Object payload = message.getPayload();
                System.out
                        .println("🔍 Payload type: " + (payload != null ? payload.getClass().getSimpleName() : "null"));

                if (payload instanceof byte[]) {
                    byte[] payloadBytes = (byte[]) payload;

                    // Calculate correct content-length using UTF-8 encoding
                    String payloadString = new String(payloadBytes, java.nio.charset.StandardCharsets.UTF_8);
                    byte[] utf8Bytes = payloadString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                    // Set the correct content-length
                    accessor.setContentLength(utf8Bytes.length);

                    System.out.println("📦 STOMP Frame - Original Length: " + payloadBytes.length + " bytes");
                    System.out.println("📦 STOMP Frame - UTF-8 Length: " + utf8Bytes.length + " bytes");
                    System.out.println("📦 STOMP Frame - Content-Length Set: " + utf8Bytes.length + " bytes");

                    // Detailed frame inspection for debugging
                    inspectStompFrame(accessor, payloadBytes);
                    System.out.println("📦 STOMP Frame - Payload Preview: "
                            + payloadString.substring(0, Math.min(100, payloadString.length())));

                    // Log frame details for debugging
                    String destination = accessor.getDestination();
                    if (destination != null) {
                        System.out.println("📦 STOMP Frame - Destination: " + destination);
                    }
                } else if (payload instanceof String) {
                    String payloadString = (String) payload;
                    byte[] utf8Bytes = payloadString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                    // Set the correct content-length using UTF-8 encoding
                    accessor.setContentLength(utf8Bytes.length);

                    System.out.println("📦 STOMP Frame - Content-Length (String): " + utf8Bytes.length + " bytes");
                    System.out.println("📦 STOMP Frame - String Payload: "
                            + payloadString.substring(0, Math.min(100, payloadString.length())));
                }
            }

            // Handle connection frames
            if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
                System.out.println("🔌 STOMP CONNECT frame intercepted");
                // Set heartbeat if not already set
                if (accessor.getHeartbeat() == null) {
                    accessor.setHeartbeat(10000, 10000);
                }
            }

            // Handle subscription frames
            if (command == StompCommand.SUBSCRIBE) {
                String destination = accessor.getDestination();
                System.out.println("📡 STOMP SUBSCRIBE to: " + destination);
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                System.err.println("❌ STOMP Frame failed to send: " + accessor.getCommand());
            }
        }
    }

    @Override
    public boolean preReceive(MessageChannel channel) {
        return true;
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // Log incoming frames for debugging
            if (command == StompCommand.SEND) {
                String destination = accessor.getDestination();
                System.out.println("📨 STOMP SEND to: " + destination);
            }
        }

        return message;
    }

    /**
     * Detailed STOMP frame inspection for debugging
     */
    private void inspectStompFrame(StompHeaderAccessor accessor, byte[] payloadBytes) {
        System.out.println("🔍 === DETAILED STOMP FRAME INSPECTION ===");

        // Check frame structure
        String payloadString = new String(payloadBytes, java.nio.charset.StandardCharsets.UTF_8);

        // Check for null terminator
        boolean hasNullTerminator = payloadString.endsWith("\0");
        System.out.println("🔍 Frame has null terminator: " + hasNullTerminator);

        if (hasNullTerminator) {
            String contentWithoutNull = payloadString.substring(0, payloadString.length() - 1);
            System.out.println("🔍 Content without null terminator: " + contentWithoutNull.length() + " chars");
            System.out.println("🔍 Content-length header: " + accessor.getContentLength());

            // Verify content-length accuracy
            byte[] contentBytes = contentWithoutNull.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            boolean contentLengthAccurate = (accessor.getContentLength() != null &&
                    accessor.getContentLength() == contentBytes.length);
            System.out.println("🔍 Content-length accurate: " + contentLengthAccurate);

            if (!contentLengthAccurate) {
                System.out.println("⚠️ Content-length mismatch! Header: " + accessor.getContentLength() +
                        ", Actual: " + contentBytes.length);
            }
        }

        // Check for extra characters after content
        if (payloadString.length() > 1) {
            char lastChar = payloadString.charAt(payloadString.length() - 1);
            char secondLastChar = payloadString.charAt(payloadString.length() - 2);
            System.out.println("🔍 Last character: '" + (lastChar == '\0' ? "\\0" : lastChar) + "' (code: "
                    + (int) lastChar + ")");
            System.out.println("🔍 Second last character: '" + (secondLastChar == '\0' ? "\\0" : secondLastChar)
                    + "' (code: " + (int) secondLastChar + ")");
        }

        // Frame headers inspection
        System.out.println("🔍 Destination: " + accessor.getDestination());
        System.out.println("🔍 Content-Type: " + accessor.getContentType());
        System.out.println("🔍 Message ID: " + accessor.getMessageId());

        System.out.println("🔍 === END FRAME INSPECTION ===");
    }
}
