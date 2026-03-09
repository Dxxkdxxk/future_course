package com.lzlz.springboot.security.config;

import com.lzlz.springboot.security.controller.TextbookWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置：监听0.0.0.0，允许跨域（局域网跨IP）
 */
@Configuration
@EnableWebSocket
@Profile("!test") // 仅非test环境生效（测试环境自动跳过）
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器，指定访问路径，允许跨域（局域网内任意IP）
        registry.addHandler(new TextbookWebSocketHandler(), "/ws/textbook")
                .setAllowedOrigins("*") // 允许所有来源（局域网内跨IP）
                .setAllowedOriginPatterns("*"); // Spring Boot 2.4+ 推荐用这个
    }

    /**
     * 注入ServerEndpointExporter，使@ServerEndpoint注解生效
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

