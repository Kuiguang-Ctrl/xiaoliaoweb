package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_story")
public class M4Story {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 所属人生时光节点（可选） */
    private Long nodeId;

    /** 所属年代记忆（可选） */
    private Long eraId;

    private Long photoId;

    private String title;

    /** 老人口述原文（Service 层 AES-GCM 加密落库） */
    private String originalText;

    /** AI 润色版（Service 层 AES-GCM 加密落库） */
    private String polishedText;

    private String summary;

    /** 讲完情绪：happy/flat/sad */
    private String mood;

    /** 背景音乐标识（可选） */
    private String music;

    /** 0草稿 1已收好 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
