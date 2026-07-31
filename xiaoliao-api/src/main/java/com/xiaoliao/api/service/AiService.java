package com.xiaoliao.api.service;

import com.xiaoliao.api.dto.ChatRequest;
import com.xiaoliao.api.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * AI 服务客户端 — 调用 Python AI 引擎
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestClient aiRestClient;

    /**
     * 发送对话请求到 Python AI 引擎
     */
    public ChatResponse chat(String userId, String message) {
        ChatRequest request = ChatRequest.builder()
                .userId(userId)
                .message(message)
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
