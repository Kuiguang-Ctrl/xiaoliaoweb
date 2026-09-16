package com.xiaoliao.api.chat;

import com.xiaoliao.api.chat.dto.AiChatRequest;
import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.api.metrics.ApiMetric;
import com.xiaoliao.common.dto.Result;
import com.xiaoliao.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一 AI 对话（后端 DeepSeek 通道）— 前端不再直连 DeepSeek，Key 收归服务端。
 * <p>system 为空 → 后端按小辽人设+RAG 组装（小程序，与之前一模一样）；
 * 传入 system → 原样透传（网页版，前端拼好的提示词行为不变）。
 */
@Slf4j
@Tag(name = "AI 对话（后端通道）", description = "DeepSeek 统一走后端：/api/ai/chat")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final DeepSeekChatService deepSeekChatService;

    @Operation(summary = "AI 对话", description = "message + conversationHistory（system 可选：为空则后端按人设+RAG 组装）")
    @PostMapping("/chat")
    @ApiMetric("ai.chat.deepseek")
    public Result<Map<String, Object>> chat(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestBody AiChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty()) {
            throw new BusinessException(400, "消息不能为空");
        }
        long start = System.currentTimeMillis();
        String reply = deepSeekChatService.chat(message, request.getConversationHistory(),
                request.getSystem(), request.getMaxTokens(), request.getTemperature());
        log.info("[AI-BACKEND] userId={} 耗时={}ms 问={} 答={}", userId, System.currentTimeMillis() - start,
                message, reply.length() > 80 ? reply.substring(0, 80) + "…" : reply);
        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        return Result.ok(data);
    }
}
