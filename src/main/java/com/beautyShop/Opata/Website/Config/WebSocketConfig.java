package com.beautyShop.Opata.Website.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Clients subscribe to topics here to receive messages
        registry.enableSimpleBroker("/topic", "/queue");

        // All messages sent FROM client must be prefixed with /app
        registry.setApplicationDestinationPrefixes("/app");

        // For private messages (user-specific queues)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The WebSocket handshake endpoint
        // Frontend connects to: ws://localhost:8080/ws/chat
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*") // replace with your frontend URL in production
            .withSockJS(); // fallback for browsers that don't support WebSocket
    }
}