package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故事 VO
 */
@Data
public class StoryVO {

    private Long id;

    private Long nodeId;

    private Long eraId;

    private Long photoId;

    private String title;

    private String originalText;

    private String polishedText;

    private String summary;

    private String mood;

    private String music;

    private Integer status;

    private LocalDateTime createTime;
}