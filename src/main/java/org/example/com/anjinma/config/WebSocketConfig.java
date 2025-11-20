package org.example.com.anjinma.config;

import java.util.HashMap;
import java.util.Map;
import org.example.com.anjinma.translation.TranslationHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub");
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/lecture")
            .setAllowedOriginPatterns(
                "https://anjinma.bluerack.org",
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*"
            )
            .withSockJS();
    }

    // Raw WebSocket endpoint for real-time multi-output translation at "/ws"
    @Bean
    public SimpleUrlHandlerMapping translationWsMapping(TranslationHandler translationHandler) {
        Map<String, Object> map = new HashMap<>();
        map.put("/ws", translationHandler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setOrder(-1);
        mapping.setUrlMap(map);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
