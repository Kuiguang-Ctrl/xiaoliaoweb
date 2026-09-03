package com.xiaoliao.api.chat;

import com.xiaoliao.api.config.ChatWsInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点：/ws/chat?token=<JWT>
 * <p>小程序连接：wss://<域名>/ws/chat?token=xxx
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ChatWsConfig implements WebSocketConfigurer {

    private final ChatWsHandler chatWsHandler;
    private final ChatWsInterceptor chatWsInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWsHandler, "/ws/chat")
                .addInterceptors(chatWsInterceptor)
                .setAllowedOrigins("*");
    }
}
