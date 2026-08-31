package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 朋友圈文案 VO
 */
@Data
public class MomentVO {

    private Long id;

    private Long photoId;

    private Long storyId;

    private String content;

    private String style;

    private Integer selected;

    private LocalDateTime createTime;
}