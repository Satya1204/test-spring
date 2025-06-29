package com.example.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Custom STOMP Frame Handler to ensure proper null termination
 * Fixes STOMP protocol compliance for Flutter clients
 */
@Component
public class StompFrameHandler extends WebSocketHandlerDecorator {

    public StompFrameHandler(WebSocketHandler delegate) {
        super(delegate);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("🔌 STOMP Frame Handler - Connection established: " + session.getId());
        super.afterConnectionEstablished(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String payload = textMessage.getPayload();
            
            System.out.println("📨 STOMP Frame Handler - Received message: " + payload.substring(0, Math.min(100, payload.length())));
            
            // Ensure incoming STOMP frames are properly handled
            if (!payload.endsWith("\0")) {
                System.out.println("⚠️ STOMP Frame Handler - Adding missing null terminator to incoming frame");
                payload = payload + "\0";
                message = new TextMessage(payload);
            }
        }
        
        super.handleMessage(session, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ STOMP Frame Handler - Transport error: " + exception.getMessage());
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("🔌 STOMP Frame Handler - Connection closed: " + session.getId() + " - " + closeStatus);
        super.afterConnectionClosed(session, closeStatus);
    }

    /**
     * Ensures STOMP frame has proper null termination
     */
    public static String ensureStompFrameTermination(String frame) {
        if (frame == null || frame.isEmpty()) {
            return frame;
        }
        
        // Check if frame already ends with null terminator
        if (!frame.endsWith("\0")) {
            System.out.println("🔧 STOMP Frame Handler - Adding null terminator to frame");
            return frame + "\0";
        }
        
        return frame;
    }

    /**
     * Creates a properly formatted STOMP frame with null termination
     */
    public static String createStompFrame(StompCommand command, String destination, String contentType, String body) {
        StringBuilder frame = new StringBuilder();
        
        // Add command
        frame.append(command.name()).append("\n");
        
        // Add headers
        if (destination != null) {
            frame.append("destination:").append(destination).append("\n");
        }
        
        if (contentType != null) {
            frame.append("content-type:").append(contentType).append("\n");
        }
        
        // Add content-length header for body
        if (body != null) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            frame.append("content-length:").append(bodyBytes.length).append("\n");
        }
        
        // Add empty line to separate headers from body
        frame.append("\n");
        
        // Add body
        if (body != null) {
            frame.append(body);
        }
        
        // Add null terminator (STOMP protocol requirement)
        frame.append("\0");
        
        System.out.println("🔧 STOMP Frame Handler - Created frame with null terminator: " + frame.length() + " bytes");
        
        return frame.toString();
    }
}
