package com.xiaoliao.api.wecom;

import com.xiaoliao.api.chat.AiService;
import com.xiaoliao.api.chat.dto.ChatResponse;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.common.enums.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 企微消息异步处理 — 避免阻塞回调 5 秒响应
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComAsyncHandler {

    private final AiService aiService;
    private final WeComMessageService msgService;
    private final UserService userService;

    /**
     * 异步处理用户消息：注册/查询用户 → 调 AI → 发回复
     */
    @Async
    public void handle(WxCpXmlMessage msg, String openKfid, String appId) {
        String externalUserId = msg.getExternalUserId();
        String content = msg.getContent();

        log.info("异步处理消息: externalUserId={}, content={}", externalUserId, content);

        try {
            // 0. 查询或自动注册用户
            User user = userService.getOrCreateUser(externalUserId);
            String userId = user.getId();
            String token = userService.generateToken(userId);

            // 1. 调 Python AI 引擎（传真实 userId）
            ChatResponse aiResp = aiService.chat(userId, content);

            // 2. 发文字 + 小程序卡片（token 随卡片链接带过去）
            IntentType intent = IntentType.fromCode(aiResp.getIntent());
            msgService.sendIntentReply(externalUserId, openKfid, aiResp.getReply(),
                    intent, appId, token);

            log.info("异步回复完成: userId={}, intent={}", userId, intent);

        } catch (Exception e) {
            log.error("异步处理消息异常: externalUserId={}", externalUserId, e);
            // 兜底：发一条友好提示
            msgService.sendText(externalUserId, openKfid, "小辽正在思考，请稍后再试～");
        }
    }
}
