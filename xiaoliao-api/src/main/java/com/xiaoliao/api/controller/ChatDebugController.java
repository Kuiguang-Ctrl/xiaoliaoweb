package com.xiaoliao.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoliao.api.dto.ChatResponse;
import com.xiaoliao.api.service.AiService;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【临时】联调调试接口 — 从 Java 后端触发 AI 对话，验证 AiService 反序列化与多轮对话
 * 联调完成后删除本文件
 */
@Tag(name = "联调调试", description = "临时接口，联调验证 Java 调用 Python AI 引擎，测完删除")
@RestController
@RequiredArgsConstructor
public class ChatDebugController {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "AI 对话调试", description = "从 Java 后端触发一次 AI 对话；history 可传历史消息模拟多轮。返回回复文本、意图与质检结果")
    @GetMapping("/debug/chat")
    public Result<Map<String, Object>> chat(
            @Parameter(description = "用户说的话") @RequestParam String message,
            @Parameter(description = "可选：历史对话 JSON 数组，如 [{\"role\":\"user\",\"content\":\"我有点累\"},{\"role\":\"assistant\",\"content\":\"先记一下心情吧？\"}]")
            @RequestParam(required = false) String history) {

        List<Map<String, String>> historyList = new ArrayList<>();
        if (history != null && !history.isBlank()) {
            try {
                historyList = objectMapper.readValue(history, new TypeReference<List<Map<String, String>>>() {
                });
            } catch (Exception e) {
                return Result.fail("history 格式错误，应为 JSON 数组，例如 [{\"role\":\"user\",\"content\":\"你好\"}]");
            }
        }

        ChatResponse resp = aiService.chat("debug_user", message, historyList);
        Map<String, Object> data = new HashMap<>();
        data.put("reply", resp.getReply());
        data.put("intent", resp.getIntent());
        data.put("inspection", resp.getInspection());
        data.put("retrievedContexts", resp.getRetrievedContexts());
        data.put("history", historyList);
        return Result.ok(data);
    }
}
