package com.example.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * STOMP Protocol Handler to ensure proper frame termination
 * Fixes STOMP protocol compliance by adding null terminators
 */
@Component
public class StompProtocolHandler implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // Handle outbound MESSAGE frames (server to client)
            if (command == StompCommand.MESSAGE) {
                System.out.println("🔧 STOMP Protocol Handler - Processing MESSAGE frame for null termination");

                Object payload = message.getPayload();

                if (payload instanceof byte[]) {
                    byte[] payloadBytes = (byte[]) payload;

                    // Convert to string to check for null termination
                    String payloadString = new String(payloadBytes, StandardCharsets.UTF_8);

                    // Ensure the frame ends with null terminator
                    if (!payloadString.endsWith("\0")) {
                        System.out.println("🔧 STOMP Protocol Handler - Adding null terminator to MESSAGE frame");

                        // Add null terminator
                        String terminatedPayload = payloadString + "\0";
                        byte[] terminatedBytes = terminatedPayload.getBytes(StandardCharsets.UTF_8);

                        // Create new accessor with updated content-length (avoid immutable header
                        // issue)
                        StompHeaderAccessor newAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
                        // Copy essential headers
                        if (accessor.getDestination() != null) {
                            newAccessor.setDestination(accessor.getDestination());
                        }
                        if (accessor.getContentType() != null) {
                            newAccessor.setContentType(accessor.getContentType());
                        }
                        newAccessor.setContentLength(terminatedBytes.length - 1); // Content-length excludes null
                                                                                  // terminator

                        // Create new message with terminated payload and updated headers
                        message = MessageBuilder.createMessage(terminatedBytes, newAccessor.getMessageHeaders());

                        System.out.println("✅ STOMP Protocol Handler - Null terminator added, frame size: "
                                + terminatedBytes.length + " bytes");
                    } else {
                        System.out.println("✅ STOMP Protocol Handler - Frame already has null terminator");
                    }
                } else if (payload instanceof String) {
                    String payloadString = (String) payload;

                    // Ensure the frame ends with null terminator
                    if (!payloadString.endsWith("\0")) {
                        System.out
                                .println("🔧 STOMP Protocol Handler - Adding null terminator to STRING MESSAGE frame");

                        // Add null terminator
                        String terminatedPayload = payloadString + "\0";
                        byte[] terminatedBytes = terminatedPayload.getBytes(StandardCharsets.UTF_8);

                        // Create new accessor with updated content-length (avoid immutable header
                        // issue)
                        StompHeaderAccessor newAccessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
                        // Copy essential headers
                        if (accessor.getDestination() != null) {
                            newAccessor.setDestination(accessor.getDestination());
                        }
                        if (accessor.getContentType() != null) {
                            newAccessor.setContentType(accessor.getContentType());
                        }
                        newAccessor.setContentLength(terminatedBytes.length - 1); // Content-length excludes null
                                                                                  // terminator

                        // Create new message with terminated payload and updated headers
                        message = MessageBuilder.createMessage(terminatedBytes, newAccessor.getMessageHeaders());

                        System.out.println("✅ STOMP Protocol Handler - Null terminator added to string frame");
                    }
                }
            }

            // Handle other frame types that might need null termination
            else if (command == StompCommand.CONNECTED || command == StompCommand.ERROR) {
                System.out.println("🔧 STOMP Protocol Handler - Processing " + command + " frame");

                Object payload = message.getPayload();
                if (payload instanceof byte[]) {
                    byte[] payloadBytes = (byte[]) payload;
                    String payloadString = new String(payloadBytes, StandardCharsets.UTF_8);

                    if (!payloadString.endsWith("\0")) {
                        System.out
                                .println("🔧 STOMP Protocol Handler - Adding null terminator to " + command + " frame");

                        String terminatedPayload = payloadString + "\0";
                        byte[] terminatedBytes = terminatedPayload.getBytes(StandardCharsets.UTF_8);

                        message = MessageBuilder.createMessage(terminatedBytes, message.getHeaders());

                        System.out.println("✅ STOMP Protocol Handler - " + command + " frame terminated");
                    }
                }
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null && accessor.getCommand() == StompCommand.MESSAGE) {
                System.out
                        .println("✅ STOMP Protocol Handler - MESSAGE frame sent successfully with proper termination");
            }
        } else {
            System.err.println("❌ STOMP Protocol Handler - Frame failed to send");
        }
    }

    /**
     * Utility method to ensure any STOMP frame has proper null termination
     */
    public static byte[] ensureStompFrameTermination(byte[] frameBytes) {
        if (frameBytes == null || frameBytes.length == 0) {
            return frameBytes;
        }

        // Check if frame already ends with null terminator
        if (frameBytes[frameBytes.length - 1] != 0) {
            System.out.println("🔧 STOMP Protocol Handler - Adding null terminator to frame bytes");

            // Create new array with null terminator
            byte[] terminatedFrame = new byte[frameBytes.length + 1];
            System.arraycopy(frameBytes, 0, terminatedFrame, 0, frameBytes.length);
            terminatedFrame[frameBytes.length] = 0; // Add null terminator

            return terminatedFrame;
        }

        return frameBytes;
    }
}
