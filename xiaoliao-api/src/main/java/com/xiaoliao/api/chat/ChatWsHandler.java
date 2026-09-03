package com.xiaoliao.api.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoliao.api.config.AuthContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket：维护 用户 -> 在线连接 映射，供后端主动推送（双向通讯）。
 * <p>协议：客户端定时发 {"type":"ping"}，服务端回 {"type":"pong"}；
 * 服务端推送主动消息 {"type":"proactive","id":..,"scene":..,"content":..,"createdAt":..}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWsHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** 每个用户可多端在线（手机 + 调试工具），用 Set 保存 */
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get(AuthContext.USER_ID_ATTR);
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("[WS] 连接建立 userId={} 在线数={}", userId, onlineCount());
        sendJson(session, Map.of("type", "connected", "userId", userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            Map<String, Object> msg = objectMapper.readValue(payload, new TypeReference<>() {
            });
            if ("ping".equals(msg.get("type"))) {
                sendJson(session, Map.of("type", "pong"));
            }
        } catch (Exception e) {
            log.debug("[WS] 忽略无法解析的消息: {}", payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get(AuthContext.USER_ID_ATTR);
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(userId);
            }
        }
        log.info("[WS] 连接关闭 userId={}", userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[WS] 传输错误 userId={} 根因={}",
                session.getAttributes().get(AuthContext.USER_ID_ATTR), exception.getMessage());
    }

    /** 推 JSON 给某用户所有在线连接；返回是否有连接成功收到 */
    public boolean sendToUser(String userId, Object payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return false;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[WS] 序列化失败 userId={}", userId, e);
            return false;
        }
        boolean sent = false;
        for (WebSocketSession session : set) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                        sent = true;
                    }
                }
            } catch (Exception e) {
                log.warn("[WS] 推送失败，移除连接 userId={}", userId, e);
                set.remove(session);
            }
        }
        return sent;
    }

    /** 当前在线用户（至少一个连接） */
    public Set<String> onlineUsers() {
        return new HashSet<>(sessions.keySet());
    }

    public int onlineCount() {
        return sessions.values().stream().mapToInt(Set::size).sum();
    }

    private void sendJson(WebSocketSession session, Object payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                }
            }
        } catch (Exception e) {
            log.warn("[WS] 发送失败 {}", e.getMessage());
        }
    }
}
