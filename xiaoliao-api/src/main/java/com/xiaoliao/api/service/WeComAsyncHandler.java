package com.xiaoliao.api.service;

import com.xiaoliao.api.dto.ChatResponse;
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

    /**
     * 异步处理用户消息：调 AI → 发回复
     */
    @Async
    public void handle(WxCpXmlMessage msg, String openKfid, String appId) {
        String openid = msg.getExternalUserId();
        String content = msg.getContent();

        log.info("异步处理消息: openid={}, content={}", openid, content);

        try {
            // 1. 调 Python AI 引擎
            ChatResponse aiResp = aiService.chat(openid, content);

            // 2. 发文字 + 小程序卡片
            IntentType intent = IntentType.fromCode(aiResp.getIntent());
            msgService.sendIntentReply(openid, openKfid, aiResp.getReply(),
                    intent, appId, null);

            log.info("异��回复完成: openid={}, intent={}", openid, intent);

        } catch (Exception e) {
            log.error("异步处理消息异常: openid={}", openid, e);
            // 兜底：发一条友好提示
            msgService.sendText(openid, openKfid, "小辽正在思考，请稍后再试～");
        }
    }
}
