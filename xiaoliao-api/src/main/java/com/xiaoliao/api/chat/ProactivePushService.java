package com.xiaoliao.api.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.chat.entity.ProactiveMessage;
import com.xiaoliao.api.chat.repository.ProactiveMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 主动消息下发：先落库（离线也能补拉）+ 在线 WebSocket 实时推送。
 * <p>推送帧：{"type":"proactive","id":..,"scene":..,"content":..,"createdAt":..}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProactivePushService {

    private final ProactiveMessageMapper mapper;
    private final ChatWsHandler chatWsHandler;

    /** 生成/推送一条主动消息：先落库再推 WebSocket（在线实时到达，离线仅入库） */
    public ProactiveMessage pushAndSave(String userId, String scene, String content) {
        ProactiveMessage msg = new ProactiveMessage();
        msg.setUserId(userId);
        msg.setScene(scene == null || scene.isBlank() ? "greeting" : scene);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        mapper.insert(msg);

        boolean online = chatWsHandler.sendToUser(userId, Map.of(
                "type", "proactive",
                "id", msg.getId(),
                "scene", msg.getScene(),
                "content", msg.getContent(),
                "createdAt", msg.getCreatedAt().toString()));
        log.info("[主动消息] 入库 userId={} scene={} id={} 在线推送={}", userId, msg.getScene(), msg.getId(), online);
        return msg;
    }

    /** 拉取某用户 afterId 之后的主动消息（聊天页打开补拉，防 WebSocket 掉线丢消息） */
    public List<ProactiveMessage> listAfter(String userId, Long afterId) {
        LambdaQueryWrapper<ProactiveMessage> wrapper = new LambdaQueryWrapper<ProactiveMessage>()
                .eq(ProactiveMessage::getUserId, userId)
                // 主动消息按天展示：只补拉当天，历史脏数据不会反复出现
                .ge(ProactiveMessage::getCreatedAt, LocalDate.now().atStartOfDay())
                .orderByAsc(ProactiveMessage::getId);
        if (afterId != null && afterId > 0) {
            wrapper.gt(ProactiveMessage::getId, afterId);
        }
        return mapper.selectList(wrapper).stream()
                .filter(m -> !ProactiveMessageService.isBadContent(m.getContent()))
                .toList();
    }
}
