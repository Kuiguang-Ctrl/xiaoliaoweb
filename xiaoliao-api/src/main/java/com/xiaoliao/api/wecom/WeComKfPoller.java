package com.xiaoliao.api.wecom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpKfService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.kf.WxCpKfMsgListResp;
import me.chanjar.weixin.cp.bean.kf.WxCpKfMsgListResp.WxCpKfMsgItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 微信客服消息轮询 —— 不依赖回调 URL，定时拉取客户消息并交给异步处理器回复。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComKfPoller {

    private static final int SYNC_LIMIT = 100;

    private final WxCpService wxCpService;
    private final WeComAsyncHandler asyncHandler;

    @Value("${wecom.kf.open-kfid}")
    private String openKfid;

    @Value("${wecom.miniprogram.appid:}")
    private String appId;

    @Value("${wecom.poll.enabled:true}")
    private boolean pollEnabled;

    /** 增量拉取游标（内存态，重启后重新从最新拉取） */
    private String cursor = "";

    /** 首次启动只推进游标、不处理历史消息，避免重启后重复回复旧消息 */
    private volatile boolean cursorInitialized = false;

    @Scheduled(fixedDelayString = "${wecom.poll.interval-ms:3000}")
    public void pollOnce() {
        if (!pollEnabled || openKfid == null || openKfid.isBlank()) {
            return;
        }
        try {
            WxCpKfService kf = wxCpService.getKfService();
            WxCpKfMsgListResp resp = kf.syncMsg(cursor, null, SYNC_LIMIT, 0, openKfid);
            if (resp == null || !resp.success() || resp.getMsgList() == null) {
                log.warn("微信客服轮询 sync_msg 失败: errcode={}, errmsg={}",
                        resp == null ? null : resp.getErrcode(),
                        resp == null ? null : resp.getErrmsg());
                return;
            }
            List<WxCpKfMsgItem> items = resp.getMsgList();
            if (!cursorInitialized) {
                cursorInitialized = true;
                log.info("微信客服轮询初始化: 推进游标，跳过历史消息 {} 条", items == null ? 0 : items.size());
            } else if (items != null && !items.isEmpty()) {
                int processed = asyncHandler.handleCustomerMessages(items, openKfid, appId);
                log.info("微信客服轮询: 拉取 {} 条，处理 {} 条", items.size(), processed);
            }
            String next = resp.getNextCursor();
            if (next != null && !next.isBlank()) {
                cursor = next;
            }
        } catch (Exception e) {
            log.warn("微信客服轮询异常", e);
        }
    }
}