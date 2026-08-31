package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 照片 VO
 */
@Data
public class PhotoVO {

    private Long id;

    private String source;

    private String url;

    private Integer isIllustration;

    private String eraTag;

    private Long nodeId;

    private Long eraId;

    private LocalDateTime createTime;
}