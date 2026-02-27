package com.beautyShop.Opata.Website.Config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    // Fired when a client connects to the WebSocket
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("🔌 WebSocket connected. Session ID: " + headerAccessor.getSessionId());
    }

    // Fired when a client disconnects
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("🔌 WebSocket disconnected. Session ID: " + headerAccessor.getSessionId());
    }
}