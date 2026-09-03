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
import java.util.Set;

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
        // 引擎正式合同 POST /v1/chat：user_id/session_id 必填，context 只接受 consent/user_summary
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("session_id", sessionId != null && !sessionId.isBlank() ? sessionId : userId);
        body.put("message", message);
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> consent = new HashMap<>();
        consent.put("personalization", false);
        context.put("consent", consent);
        context.put("user_summary", "");
        body.put("context", context);
        body.put("conversation_history", sanitizeHistory(conversationHistory));
        body.put("debug", false);
        if (images != null && !images.isEmpty()) {
            body.put("images", images);
        }

        try {
            long start = System.currentTimeMillis();
            Map<String, Object> resp = aiRestClient.post()
                    .uri("/v1/chat")
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            String reply = resp == null ? null : (String) resp.get("reply");
            String intent = resp == null ? null : (String) resp.get("intent");
            log.info("[AI] 调用成功 userId={} message={} 耗时={}ms reply={} intent={}",
                    userId, message, System.currentTimeMillis() - start, reply, intent);
            // 引擎快捷建议（suggestions）只在 M4 记录类意图时渲染成可点卡片，避免普通对话被卡片刷屏
            ChatResponse response = ChatResponse.builder()
                    .reply(reply)
                    .intent(intent)
                    .card(isM4RecordIntent(intent) ? buildCardFromSuggestions(resp.get("suggestions")) : null)
                    .build();
            response.setRecommendation(recommend(userId, sessionId, message, conversationHistory));
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
     * 主动问候：调引擎 /v1/greeting 生成一句 AI 问候（绕开对话管线，引擎侧实现）
     * <p>引擎未实现/调用失败/返回空或坏内容时返回 null，由调用方回退本地模板。
     */
    public String greeting(String userId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", userId);
            // 告知引擎触发场景：用户刚打开聊天页，应生成引导老人开口说话的问候
            body.put("trigger", "chat_page_open");
            Map<String, Object> context = new HashMap<>();
            context.put("scene", "welcome");
            context.put("goal", "greet_and_guide");
            body.put("context", context);
            long start = System.currentTimeMillis();
            Map<String, Object> resp = aiRestClient.post()
                    .uri("/v1/greeting")
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            String reply = resp == null ? null : (String) resp.get("reply");
            if (reply == null || reply.isBlank()) {
                log.warn("[AI] 主动问候返回空，回退模板 userId={}", userId);
                return null;
            }
            log.info("[AI] 主动问候生成成功 userId={} 耗时={}ms reply={}",
                    userId, System.currentTimeMillis() - start, reply);
            return reply.trim();
        } catch (Exception e) {
            log.warn("[AI] 主动问候生成失败（引擎未实现或异常），回退模板 userId={} 根因={}",
                    userId, e.getMessage());
            return null;
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
    /** M4 记录类意图：识别到老人在回忆/讲故事/聊人生时，把引擎建议渲染成卡片 */
    private static final Set<String> M4_RECORD_INTENTS = Set.of("m4_story", "m4_era_memory", "m4_life_story");

    private static boolean isM4RecordIntent(String intent) {
        return intent != null && M4_RECORD_INTENTS.contains(intent);
    }

    /** 引擎 suggestions（{label,value,intent,module}）→ 前端 buttons 卡片；无可用项返回 null */
    private Map<String, Object> buildCardFromSuggestions(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> buttons = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> suggestion)) {
                continue;
            }
            Object label = suggestion.get("label");
            if (label == null || String.valueOf(label).isBlank()) {
                continue;
            }
            Object value = suggestion.get("value");
            Map<String, Object> button = new HashMap<>();
            button.put("key", value == null ? String.valueOf(label) : String.valueOf(value));
            button.put("label", String.valueOf(label));
            buttons.add(button);
        }
        if (buttons.isEmpty()) {
            return null;
        }
        Map<String, Object> card = new HashMap<>();
        card.put("type", "buttons");
        card.put("title", "小辽可以帮您：");
        card.put("buttons", buttons);
        return card;
    }

    /** /v1/chat 只接受 user/assistant 角色且内容非空的最近 30 条历史，坏项丢弃或兜底为 [图片] */
    private List<Map<String, String>> sanitizeHistory(List<Map<String, String>> history) {
        List<Map<String, String>> clean = new ArrayList<>();
        if (history == null) {
            return clean;
        }
        for (Map<String, String> item : history) {
            if (clean.size() >= 30) {
                break;
            }
            String role = item.get("role");
            String content = item.get("content");
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            if (content == null || content.isBlank()) {
                content = "[图片]";
            }
            Map<String, String> turn = new HashMap<>();
            turn.put("role", role);
            turn.put("content", content);
            clean.add(turn);
        }
        return clean;
    }
}
