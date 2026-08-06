package com.xiaoliao.api.chat;

import com.xiaoliao.api.chat.dto.ChatRequest;
import com.xiaoliao.api.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 服务客户端 — 调用 Python AI 引擎
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestClient aiRestClient;

    /**
     * 发送对话请求到 Python AI 引擎（无历史）
     */
    public ChatResponse chat(String userId, String message) {
        return chat(userId, message, new ArrayList<>());
    }

    /**
     * 发送对话请求到 Python AI 引擎（带历史，支持多轮）
     *
     * @param conversationHistory 历史消息，按时间顺序，每项 {role: user/assistant/system, content}
     */
    public ChatResponse chat(String userId, String message, List<Map<String, String>> conversationHistory) {
        ChatRequest request = ChatRequest.builder()
                .userId(userId)
                .message(message)
                .conversationHistory(conversationHistory != null ? conversationHistory : new ArrayList<>())
                .build();

        try {
            return aiRestClient.post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (ResourceAccessException e) {
            log.error("AI引擎连接失败", e);
            return ChatResponse.builder()
                    .reply("小辽稍微走神了，您再说一遍？")
                    .intent("chat")
                    .build();
        } catch (Exception e) {
            log.error("AI引擎调用异常", e);
            return ChatResponse.builder()
                    .reply("小辽现在有点忙，稍等一下再试试～")
                    .intent("chat")
                    .build();
        }
    }

    /**
     * 健康检查
     */
    public boolean ping() {
        try {
            aiRestClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
