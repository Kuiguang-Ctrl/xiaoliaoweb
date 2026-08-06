package com.xiaoliao.api.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话日志表 — 记录每次用户与AI的完整对话
 */
@Data
@TableName("conversation_logs")
public class ConversationLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    private String userMsg;

    private String agentReply;

    private String intent;

    /** 副Agent 5维检验原始结果 (JSONB) */
    private String inspectionJson;

    /** 错误标签数组，如 {suggested_too_early, ignored_emotion} */
    private String errorTags;

    /** 质检综合评分 1-5 */
    private Integer score;

    private LocalDateTime createdAt;
}
