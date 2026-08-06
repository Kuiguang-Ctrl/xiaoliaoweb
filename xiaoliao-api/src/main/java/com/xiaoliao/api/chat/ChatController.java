package com.xiaoliao.api.chat;

import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.chat.dto.ChatRequest;
import com.xiaoliao.api.chat.dto.ChatResponse;
import com.xiaoliao.api.chat.AiService;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 对话接口 — 前端小程序调 AI 客服，Java 转发给 Python 智能体引擎
 */
@Tag(name = "AI 对话", description = "前端调 AI 客服，Java 转发给 Python 引擎")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiService aiService;

    @Operation(summary = "AI 对话", description = "发送用户消息，返回 AI 回复文本、意图与质检结果。userId 由鉴权自动带入，无需传")
    @PostMapping
    public Result<Map<String, Object>> chat(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestBody ChatRequest request) {
        ChatResponse resp = aiService.chat(userId, request.getMessage(), request.getConversationHistory());
        Map<String, Object> data = new HashMap<>();
        data.put("reply", resp.getReply());
        data.put("intent", resp.getIntent());
        data.put("inspection", resp.getInspection());
        return Result.ok(data);
    }
}
