package com.xiaoliao.api.m4.vo;

import lombok.Data;

/**
 * 年代记忆 VO
 */
@Data
public class EraVO {

    private Long id;

    private String name;

    private Long coverPhotoId;

    /** 年代下视频作品数 */
    private Long videoCount;
}