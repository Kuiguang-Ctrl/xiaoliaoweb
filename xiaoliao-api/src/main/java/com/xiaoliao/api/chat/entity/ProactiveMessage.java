package com.xiaoliao.api.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 主动消息：小辽主动问候，聊天页打开时拉取展示
 */
@Data
@TableName("proactive_messages")
public class ProactiveMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String content;

    /** 场景：greeting/checkin_reminder/mood_care/inactive_recall/festival/game_recommend */
    private String scene;

    private LocalDateTime createdAt;
}
