package com.xiaoliao.api.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 主动消息定时任务：每天 9:00 给在线用户推当天问候（聊天页打开期间实时收到）；
 * 不在线用户不推，打开页面时由 /api/chat/greeting 兜底生成展示。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProactiveScheduler {

    private final ChatWsHandler chatWsHandler;
    private final ProactiveMessageService proactiveMessageService;
    private final ProactivePushService proactivePushService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void morningGreeting() {
        log.info("[主动消息] 定时问候开始，当前在线用户={}", chatWsHandler.onlineUsers());
        for (String userId : chatWsHandler.onlineUsers()) {
            try {
                if (proactiveMessageService.findTodayGreeting(userId) != null) {
                    continue; // 今天已问候过，不重复打扰
                }
                proactivePushService.pushAndSave(userId, "greeting",
                        proactiveMessageService.generateGreeting());
            } catch (Exception e) {
                log.warn("[主动消息] 定时问候失败 userId={} 根因={}", userId, e.getMessage());
            }
        }
    }
}
