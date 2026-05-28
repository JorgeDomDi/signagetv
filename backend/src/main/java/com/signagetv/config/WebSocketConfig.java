package com.signagetv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker simple en memoria; topics que publica el servidor
        config.enableSimpleBroker("/topic");
        // Prefijo para mensajes dirigidos al servidor (no usado por ahora)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint STOMP principal. Soporta tanto WebSocket nativo (Android/OkHttp)
        // como SockJS (navegador) — los clientes SockJS conectan a /ws,
        // los nativos a /ws (o /ws/websocket como transporte interno).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
