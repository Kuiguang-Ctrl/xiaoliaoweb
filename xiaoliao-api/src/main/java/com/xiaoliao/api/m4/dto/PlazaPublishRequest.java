package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布作品到广场请求
 */
@Data
public class PlazaPublishRequest {

    /** 我的视频作品 ID（m4_video.id） */
    @NotNull(message = "请选择要发到广场的作品")
    private Long videoId;

    /** 广场上显示的标题（可选，默认用作品原标题） */
    @Size(max = 64, message = "标题不能超过64字")
    private String title;
}
