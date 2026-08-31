package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存故事请求（人生时光节点下 / 年代记忆下）
 */
@Data
public class StorySaveRequest {

    /** 所属人生时光节点（可选） */
    private Long nodeId;

    /** 所属年代记忆（可选） */
    private Long eraId;

    /** 关联照片（可选） */
    private Long photoId;

    @Size(max = 64, message = "标题不能超过64字")
    private String title;

    /** 老人口述原文 */
    @NotBlank(message = "故事内容不能为空")
    @Size(max = 5000, message = "故事不能超过5000字")
    private String originalText;

    /** AI 润色版（可选） */
    private String polishedText;

    @Size(max = 200, message = "摘要不能超过200字")
    private String summary;

    /** 讲完情绪：happy/flat/sad */
    private String mood;

    /** 背景音乐标识（可选） */
    private String music;
}