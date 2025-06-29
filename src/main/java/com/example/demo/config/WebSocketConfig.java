package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompFrameInterceptor stompFrameInterceptor;

    @Autowired
    private StompMessageConverter stompMessageConverter;

    @Autowired
    private StompProtocolHandler stompProtocolHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages back to the
        // client
        config.enableSimpleBroker("/topic", "/queue");

        // Designate the "/app" prefix for messages that are bound for @MessageMapping
        // methods
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for personal messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint for native WebSocket connections (Flutter
        // compatible)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(null); // Use default handshake handler

        // SockJS endpoint for web browsers that need fallback
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(512 * 1024) // 512KB stream limit
                .setHttpMessageCacheSize(1000) // Cache 1000 messages
                .setDisconnectDelay(30 * 1000); // 30 second disconnect delay
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure WebSocket transport settings to fix STOMP frame parsing issues
        registration
                .setMessageSizeLimit(64 * 1024) // 64KB max message size
                .setSendBufferSizeLimit(512 * 1024) // 512KB send buffer
                .setSendTimeLimit(20 * 1000) // 20 second send timeout
                .setTimeToFirstMessage(30 * 1000); // 30 second first message timeout
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add interceptors for inbound messages (client to server)
        registration.interceptors(stompFrameInterceptor, stompProtocolHandler);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Add interceptors for outbound messages (server to client)
        // STOMP Protocol Handler MUST be first to ensure null termination
        registration.interceptors(stompProtocolHandler, stompFrameInterceptor);
        // Set task executor for better performance
        registration.taskExecutor().corePoolSize(4).maxPoolSize(8);
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // Clear default converters and add our custom one first
        messageConverters.clear();
        messageConverters.add(stompMessageConverter);
        System.out.println("🔧 Custom STOMP Message Converter configured (replacing defaults)");
        return true; // Don't add default converters
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("🔌 WebSocket connection established: " + event.getMessage());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        System.out.println("🔌 WebSocket connection closed: " + event.getMessage());
    }
}
