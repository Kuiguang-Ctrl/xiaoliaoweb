package com.xiaoliao.api.m4.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场作品 VO
 */
@Data
public class PlazaWorkVO {

    /** 广场作品 id（点赞/撤下用它） */
    private Long id;

    /** 来源视频作品 id */
    private Long videoId;

    private String title;

    private String videoUrl;

    private String coverUrl;

    private Integer duration;

    /** 作者 userId */
    private String ownerId;

    /** 作者昵称 */
    private String ownerName;

    /** 是不是我自己的作品 */
    private Boolean mine;

    private Integer likeCount;

    /** 我点过赞没有 */
    private Boolean liked;

    private LocalDateTime createTime;
}
