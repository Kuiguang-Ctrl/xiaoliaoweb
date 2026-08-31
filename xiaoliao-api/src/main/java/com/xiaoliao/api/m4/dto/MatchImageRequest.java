package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 年代示意图匹配请求（根据老人对"以前的样子"的描述检索素材库）
 */
@Data
public class MatchImageRequest {

    /** 老人上传的照片ID（作为语境，可选） */
    private Long photoId;

    /** 老人口述：以前是什么样子的 */
    @NotBlank(message = "描述不能为空")
    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    /** AI 引擎已提取的检索词（逗号分隔，可选；为空时按描述分词兜底） */
    private String keywords;
}