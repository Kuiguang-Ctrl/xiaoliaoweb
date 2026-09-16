package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频作品 VO
 */
@Data
public class VideoVO {

    private Long id;

    private Long nodeId;

    private Long eraId;

    private String title;

    private String caption;

    private String videoUrl;

    private String posterUrl;

    private Integer duration;

    private LocalDateTime createTime;
}
