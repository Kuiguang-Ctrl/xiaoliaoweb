package com.xiaoliao.api.wecom;

import com.xiaoliao.api.chat.AiService;
import com.xiaoliao.api.chat.dto.ChatResponse;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.common.enums.IntentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpKfService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.kf.WxCpKfMsgListResp;
import me.chanjar.weixin.cp.bean.kf.WxCpKfMsgListResp.WxCpKfMsgItem;
import me.chanjar.weixin.cp.bean.kf.WxCpKfServiceStateResp;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企微消息异步处理 — 避免阻塞回调 5 秒响应
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComAsyncHandler {

    private final AiService aiService;
    private final WeComMessageService msgService;
    private final UserService userService;
    private final WxCpService wxCpService;

    /** sync_msg 单次拉取条数上限 */
    private static final int SYNC_MSG_LIMIT = 100;
    /** sync_msg 分页最多拉几页，防异常死循环 */
    private static final int SYNC_MSG_MAX_PAGES = 5;
    /** 已处理消息去重集合，防同一 msgId 被重复回复 */
    private static final int MAX_PROCESSED_MSG_IDS = 5000;
    private final Set<String> processedMsgIds = ConcurrentHashMap.newKeySet();

    /**
     * 自建应用消息回调路径（旧）：消息正文直接随回调下发，无需 sync_msg
     */
    @Async
    public void handle(WxCpXmlMessage msg, String openKfid, String appId) {
        String externalUserId = msg.getExternalUserId();
        String content = msg.getContent();

        log.info("异步处理消息(回调直带正文): externalUserId={}, content={}", externalUserId, content);

        try {
            respondToUser(externalUserId, openKfid, content, appId);
        } catch (Exception e) {
            log.error("异步处理消息异常: externalUserId={}", externalUserId, e);
            // 兜底：发一条友好提示
            msgService.sendText(externalUserId, openKfid, "小辽正在思考，请稍后再试～");
        }
    }

    /**
     * 微信客服(kf)回调路径：回调只带 token 通知，正文需调 sync_msg 拉取。
     * 回调本身不含 external_userid，客户身份从消息列表里取。
     */
    @Async
    public void handleKfEvent(String openKfid, String callbackToken, String appId) {
        log.info("异步处理 kf 事件: openKfid={}", openKfid);

        String externalUserId = null;
        try {
            // 1. 用回调 token 调 sync_msg 拉消息，取最新一条客户文本
            KfCustomerMsg latest = fetchLatestCustomerText(openKfid, callbackToken);
            if (latest == null) {
                log.warn("sync_msg 未拉到客户文本消息: openKfid={}", openKfid);
                return;
            }
            externalUserId = latest.externalUserId;

            // 2. 走统一的 注册用户 → AI → 回复 流程
            respondToUser(latest.externalUserId, openKfid, latest.content, appId);
        } catch (Exception e) {
            log.error("异步处理 kf 事件异常: externalUserId={}", externalUserId, e);
            // 兜底：发一条友好提示
            if (externalUserId != null) {
                msgService.sendText(externalUserId, openKfid, "小辽正在思考，请稍后再试～");
            }
        }
    }

    /**
     * 统一处理：注册/查询用户 → 调 AI → 发文字 + 小程序卡片
     */
    /**
     * ?????????????????? -> ?????? -> AI ?????????????
     */
    public int handleCustomerMessages(List<WxCpKfMsgItem> items, String openKfid, String appId) {
        int processed = 0;
        for (WxCpKfMsgItem item : items) {
            try {
                if (item.getOrigin() == null || item.getOrigin() != 3) {
                    continue; // ?????????
                }
                if (!"text".equals(item.getMsgType()) || item.getText() == null) {
                    continue;
                }
                String externalUserId = item.getExternalUserId();
                String content = item.getText().getContent();
                if (externalUserId == null || externalUserId.isBlank()
                        || content == null || content.isBlank()) {
                    continue;
                }
                // ???????? msgId ?????
                if (!processedMsgIds.add(item.getMsgId())) {
                    continue;
                }
                trimProcessedMsgIds();

                if (!ensureAiServing(openKfid, externalUserId)) {
                    continue; // 人工接待/已结束，AI 无法回复，跳过本条
                }
                respondToUser(externalUserId, openKfid, content, appId);
                processed++;
            } catch (Exception e) {
                log.error("????????: msgId={}", item == null ? null : item.getMsgId(), e);
            }
        }
        return processed;
    }

    /**
     * ??????????????????/????????????????
     */
    private boolean ensureAiServing(String openKfid, String externalUserId) throws Exception {
        WxCpKfService kf = wxCpService.getKfService();
        WxCpKfServiceStateResp state = kf.getServiceState(openKfid, externalUserId);
        if (state == null || !state.success() || state.getServiceState() == null) {
            log.warn("查询会话状态失败，按可回复处理: openKfid={}, externalUserId={}", openKfid, externalUserId);
            return true;
        }
        int current = state.getServiceState();
        if (current == 1) {
            return true; // 已是智能助手接待，AI 可直接回复
        }
        if (current == 2) {
            // 待接入池排队中 → 接入智能助手接待
            kf.transServiceState(openKfid, externalUserId, 1, null);
            return true;
        }
        if (current == 3) {
            // 人工接待中，AI 无法回复：结束会话，等客户重新发消息进入新会话
            log.warn("会话处于人工接待状态(3)，AI 无法回复，结束会话等待客户重新进入: openKfid={}, externalUserId={}",
                    openKfid, externalUserId);
            kf.transServiceState(openKfid, externalUserId, 4, null);
            return false;
        }
        // 其他状态（0 未处理 / 4 已结束）：尝试直接回复
        return true;
    }

    private void respondToUser(String externalUserId, String openKfid, String content, String appId) {
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
    }

    /**
     * 调 kf/sync_msg 分页拉消息，返回最新一条客户发的文本消息。
     * origin: 3=微信客户发送，4=系统推送事件，5=接待人员发送 —— 只回客户。
     */
    private KfCustomerMsg fetchLatestCustomerText(String openKfid, String callbackToken) throws Exception {
        WxCpKfService kfService = wxCpService.getKfService();
        String cursor = null;
        KfCustomerMsg latest = null;
        long latestSendTime = -1L;

        for (int page = 0; page < SYNC_MSG_MAX_PAGES; page++) {
            WxCpKfMsgListResp resp = kfService.syncMsg(cursor, callbackToken, SYNC_MSG_LIMIT, 0, openKfid);
            if (resp == null || !resp.success() || resp.getMsgList() == null) {
                log.warn("sync_msg 失败: errcode={}, errmsg={}",
                        resp == null ? null : resp.getErrcode(),
                        resp == null ? null : resp.getErrmsg());
                break;
            }
            for (WxCpKfMsgListResp.WxCpKfMsgItem item : resp.getMsgList()) {
                if (item.getOrigin() == null || item.getOrigin() != 3) {
                    continue; // 只看客户发送的消息
                }
                if (!"text".equals(item.getMsgType()) || item.getText() == null) {
                    continue;
                }
                String externalUserId = item.getExternalUserId();
                String content = item.getText().getContent();
                if (externalUserId == null || externalUserId.isBlank()
                        || content == null || content.isBlank()) {
                    continue;
                }
                // 防重复回复：同一 msgId 只处理一次
                if (!processedMsgIds.add(item.getMsgId())) {
                    continue;
                }
                trimProcessedMsgIds();

                long sendTime = item.getSendTime() == null ? 0 : item.getSendTime();
                if (sendTime >= latestSendTime) {
                    latestSendTime = sendTime;
                    latest = new KfCustomerMsg(externalUserId, content);
                }
            }
            // has_more=1 且有下一页才继续
            if (resp.getHasMore() != null && resp.getHasMore() == 1
                    && resp.getNextCursor() != null && !resp.getNextCursor().isBlank()) {
                cursor = resp.getNextCursor();
            } else {
                break;
            }
        }
        return latest;
    }

    /**
     * 去重集合超过上限时整体清空（防内存泄漏；代价是极低概率的重复回复）
     */
    private void trimProcessedMsgIds() {
        if (processedMsgIds.size() > MAX_PROCESSED_MSG_IDS) {
            processedMsgIds.clear();
        }
    }

    /** 客户文本消息：externalUserId + 正文 */
    private record KfCustomerMsg(String externalUserId, String content) {}
}