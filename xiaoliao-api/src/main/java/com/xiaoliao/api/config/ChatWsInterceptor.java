package com.xiaoliao.api.config;

import com.xiaoliao.api.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手鉴权：小程序 WebSocket 不能带自定义 Header，token 走 query 参数
 * {@code /ws/chat?token=<JWT>}，逻辑与 AuthInterceptor 一致（dev 关闭鉴权时用 mock 用户）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWsInterceptor implements HandshakeInterceptor {

    private final TokenUtil tokenUtil;

    /** 鉴权开关：true=校验 token；false=跳过（联调用） */
    @Value("${xiaoliao.auth.enabled:true}")
    private boolean authEnabled;

    /** 跳过鉴权时使用的固定用户 id */
    @Value("${xiaoliao.auth.mock-user-id:}")
    private String mockUserId;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        if (token == null && request.getURI().getQuery() != null) {
            for (String pair : request.getURI().getQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) {
                    token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        }

        String userId = (token == null || token.isBlank()) ? null : tokenUtil.parseUserId(token);
        if (authEnabled) {
            if (userId == null) {
                log.warn("[WS] 握手拒绝：token 无效 uri={}", request.getURI());
                return false;
            }
        } else {
            userId = (userId != null) ? userId : mockUserId;
            log.warn("[WS] 鉴权已关闭(dev)，userId={}", userId);
        }
        attributes.put(AuthContext.USER_ID_ATTR, userId);
        log.info("[WS] 握手成功 userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
