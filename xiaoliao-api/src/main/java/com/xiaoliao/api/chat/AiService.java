package com.xiaoliao.api.chat;

import com.xiaoliao.api.chat.dto.ChatRequest;
import com.xiaoliao.api.chat.dto.ChatResponse;
import com.xiaoliao.api.metrics.ApiMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
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
        return chat(userId, message, conversationHistory, null, null);
    }

    /**
     * 发送对话请求到 Python AI 引擎（带历史、会话ID，支持多轮）
     *
     * @param conversationHistory 历史消息，按时间顺序，每项 {role: user/assistant/system, content}
     * @param sessionId           会话ID：同一会话只弹一次推荐，可为 null
     */
    public ChatResponse chat(String userId, String message, List<Map<String, String>> conversationHistory, String sessionId) {
        return chat(userId, message, conversationHistory, sessionId, null);
    }

    /**
     * 发送对话请求到 Python AI 引擎（带历史、会话ID、图片，支持多轮）
     *
     * @param conversationHistory 历史消息，按时间顺序，每项 {role: user/assistant/system, content}
     * @param sessionId           会话ID：同一会话只弹一次推荐，可为 null
     * @param images              本轮图片 URL 列表（引擎支持多模态后生效；为空时不传该字段）
     */
    @ApiMetric("ai.engine.chat")
    public ChatResponse chat(String userId, String message, List<Map<String, String>> conversationHistory,
                             String sessionId, List<String> images) {
        ChatRequest.ChatRequestBuilder builder = ChatRequest.builder()
                .userId(userId)
                .message(message)
                .sessionId(sessionId)
                .conversationHistory(conversationHistory != null ? conversationHistory : new ArrayList<>());
        if (images != null && !images.isEmpty()) {
            builder.images(images);
        }
        ChatRequest request = builder.build();

        try {
            long start = System.currentTimeMillis();
            ChatResponse response = aiRestClient.post()
                    .uri("/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
            log.info("[AI] 调用成功 userId={} message={} 耗时={}ms reply={} intent={}",
                    userId, message, System.currentTimeMillis() - start,
                    response != null ? response.getReply() : null,
                    response != null ? response.getIntent() : null);
            if (response != null) {
                response.setRecommendation(recommend(userId, sessionId, message, request.getConversationHistory()));
            }
            return response;
        } catch (ResourceAccessException e) {
            log.error("[AI] 调用失败-引擎连接不上（Connection refused/超时），请检查 AI 引擎是否启动 userId={} message={} 根因={}",
                    userId, message, e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("小辽稍微走神了，您再说一遍？")
                    .intent("chat")
                    .build();
        } catch (Exception e) {
            log.error("[AI] 调用失败-引擎处理异常（引擎报错/返回格式错误）userId={} message={} 根因={}",
                    userId, message, e.getMessage(), e);
            return ChatResponse.builder()
                    .reply("小辽现在有点忙，稍等一下再试试～")
                    .intent("chat")
                    .build();
        }
    }

    /**
     * 调用引擎意图推荐接口 /v1/intent/recommend，判断本轮是否推荐进入小程序
     * 引擎未实现该接口或调用失败时返回 null，不影响正常对话
     */
    private Map<String, Object> recommend(String userId, String sessionId, String message,
                                          List<Map<String, String>> conversationHistory) {
        try {
            Map<String, Object> consent = new HashMap<>();
            consent.put("personalization", false);
            Map<String, Object> context = new HashMap<>();
            context.put("consent", consent);
            context.put("user_summary", "");

            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            body.put("session_id", sessionId != null ? sessionId : "");
            body.put("message", message);
            body.put("context", context);
            body.put("conversation_history", conversationHistory != null ? conversationHistory : new ArrayList<>());
            body.put("debug", false);

            long start = System.currentTimeMillis();
            Map<String, Object> result = aiRestClient.post()
                    .uri("/v1/intent/recommend")
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            log.info("[AI] 意图推荐 userId={} sessionId={} 耗时={}ms recommended={}",
                    userId, sessionId, System.currentTimeMillis() - start,
                    result != null ? result.get("recommended") : null);
            return result;
        } catch (Exception e) {
            // 引擎未实现该接口或推荐服务异常时，静默跳过，不影响对话主流程
            log.warn("[AI] 意图推荐不可用（引擎可能未实现 /v1/intent/recommend），跳过推荐 userId={} 根因={}",
                    userId, e.getMessage());
            return null;
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
