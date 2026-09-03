package com.xiaoliao.api.chat;

import com.xiaoliao.api.chat.entity.ProactiveMessage;
import com.xiaoliao.api.chat.dto.PushRequest;
import com.xiaoliao.api.config.AuthContext;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 主动消息接口：聊天页打开时拉取小辽问候
 */
@Tag(name = "AI 主动消息", description = "小辽主动问候，聊天页打开时调用")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ProactiveController {

    private final ProactiveMessageService proactiveMessageService;
    private final ProactivePushService proactivePushService;

    @Operation(summary = "获取小辽主动问候", description = "聊天页打开时调用；当天已问候过则返回当天那条，不会重复生成")
    @GetMapping("/greeting")
    public Result<ProactiveMessage> greeting(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId) {
        return Result.ok(proactiveMessageService.getOrCreateTodayGreeting(userId));
    }

    @Operation(summary = "拉取主动消息", description = "聊天页打开时补拉 afterId 之后的主动消息（WebSocket 掉线兜底）")
    @GetMapping("/pushes")
    public Result<List<ProactiveMessage>> pushes(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestParam(defaultValue = "0") Long afterId) {
        return Result.ok(proactivePushService.listAfter(userId, afterId));
    }

    @Operation(summary = "推送一条主动消息（测试/运营用）", description = "给当前用户推一条：在线走 WebSocket 实时到达，离线仅入库")
    @PostMapping("/push")
    public Result<ProactiveMessage> push(
            @Parameter(hidden = true) @RequestAttribute(AuthContext.USER_ID_ATTR) String userId,
            @RequestBody @Valid PushRequest request) {
        return Result.ok(proactivePushService.pushAndSave(userId, request.getScene(), request.getContent()));
    }
}
