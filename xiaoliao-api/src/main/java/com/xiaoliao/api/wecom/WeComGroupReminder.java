package com.xiaoliao.api.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.api.user.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客服定时提醒 —— 每天按 cron 推送提醒，支持两个通道：
 * <p>
 * 1. 内部群 webhook 广播（配了 webhook 才发；群机器人 webhook 只支持内部群）
 * 2. 微信客服 1:1 私发（发给最近 window-hours 小时内聊过天的客户，平台 48 小时规则）
 * <p>
 * Redis 记录「日期+目标」防一天重复发送，重启也不会重发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComGroupReminder {

    private static final String REMINDER_KEY_PREFIX = "kf:reminder:";
    private static final String WEBHOOK_KEY_SUFFIX = ":webhook";
    private static final int MIN_WINDOW_HOURS = 1;

    private final UserMapper userMapper;
    private final WeComMessageService msgService;
    private final StringRedisTemplate redisTemplate;

    @Value("${wecom.group-reminder.enabled:false}")
    private boolean enabled;

    @Value("${wecom.group-reminder.webhook:}")
    private String webhook;

    @Value("${wecom.group-reminder.content:现在是九点，该打卡啦～ 需要帮助可以点击群里的「客服助理」}")
    private String content;

    @Value("${wecom.group-reminder.webhook-content:}")
    private String webhookContent;

    @Value("${wecom.kf.open-kfid:}")
    private String openKfid;

    @Value("${wecom.group-reminder.window-hours:48}")
    private int windowHours;

    @Scheduled(cron = "${wecom.group-reminder.cron:0 0 9 * * ?}")
    public void sendDailyReminder() {
        if (!enabled) {
            return;
        }
        sendWebhookReminder();
        sendKfReminder();
    }

    /** 内部群 webhook 广播（每天一次，Redis 防重） */
    private void sendWebhookReminder() {
        if (webhook == null || webhook.isBlank()) {
            return;
        }
        String date = LocalDate.now().toString();
        String key = REMINDER_KEY_PREFIX + date + WEBHOOK_KEY_SUFFIX;
        if (alreadySentToday(key)) {
            return;
        }
        try {
            // 群消息用 markdown：支持 [客服助理](客服链接) 可点击；没配专属文案就用纯文本内容
            String msg = webhookContent == null || webhookContent.isBlank() ? content : webhookContent;
            RestClient.create()
                    .post()
                    .uri(webhook)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("msgtype", "markdown", "markdown", Map.of("content", msg)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("群机器人定时提醒已发送: {}", msg);
        } catch (Exception e) {
            log.warn("群机器人定时提醒发送失败: {}", e.getMessage());
        }
    }

    /** 客服 1:1 私发（48 小时窗口内活跃客户） */
    private void sendKfReminder() {
        if (openKfid == null || openKfid.isBlank()) {
            return;
        }
        try {
            String date = LocalDate.now().toString();
            LocalDateTime cutoff = LocalDateTime.now().minusHours(Math.max(MIN_WINDOW_HOURS, windowHours));
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .ge(User::getLastActiveAt, cutoff));
            int sent = 0;
            for (User user : users) {
                String openid = user.getOpenid();
                if (openid == null || !openid.startsWith("wm")) {
                    continue; // 只给企微客服客户发
                }
                String key = REMINDER_KEY_PREFIX + date + ":" + user.getId();
                if (alreadySentToday(key)) {
                    continue;
                }
                msgService.sendText(openid, openKfid, content);
                sent++;
            }
            log.info("客服 1:1 定时提醒完成: 候选 {} 人，发送 {} 人", users.size(), sent);
        } catch (Exception e) {
            log.warn("客服 1:1 定时提醒执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * Redis 原子占位：第一次占上返回 true；已占/占失败都按「已发送」处理避免重复
     */
    private boolean alreadySentToday(String key) {
        try {
            return !Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofHours(30)));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过防重直接发送: {}", e.getMessage());
            return false;
        }
    }
}