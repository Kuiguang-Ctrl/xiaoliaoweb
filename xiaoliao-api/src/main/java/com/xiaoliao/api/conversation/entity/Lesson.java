package com.xiaoliao.api.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教训库 — 副Agent 聚类生成的 Prompt 补丁
 */
@Data
@TableName("lessons")
public class Lesson {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 错误模式标签 */
    private String errorTag;

    /** 生成的 Prompt 补丁内容 */
    private String patchContent;

    /** bge-large-zh 向量，用于 RAG 检索 */
    @TableField("embedding")
    private String embedding;

    /** 错误触发次数 */
    private Integer frequency;

    /** pending / verified / rejected */
    private String status;

    private LocalDateTime createdAt;
}
