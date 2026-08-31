package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 上传照片请求（老人照片或示意图）
 */
@Data
public class PhotoUploadRequest {

    /** 图片URL（上传至OSS后的地址） */
    @NotBlank(message = "图片URL不能为空")
    @Size(max = 512, message = "图片URL不能超过512字符")
    private String url;

    /** 来源：user=老人上传 stock=系统示意图 */
    @NotBlank(message = "来源不能为空")
    private String source;

    /** 是否年代示意图 0否 1是 */
    private Integer isIllustration;

    /** 年代标签，如 1960s */
    private String eraTag;

    /** 关联人生时光节点（可选） */
    private Long nodeId;

    /** 关联年代记忆（可选） */
    private Long eraId;
}