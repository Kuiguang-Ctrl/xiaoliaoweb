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

    /** 年代下故事/对照卡数 */
    private Long storyCount;
}