package com.xiaoliao.api.m3.vo;

import lombok.Data;

/**
 * 感恩留言
 */
@Data
public class GratitudeVO {

    private Long id;

    private String targetName;

    private String content;

    /** yyyy-MM-dd HH:mm */
    private String createTime;

    /** 是否已分享给家人 0未分享 1已分享 */
    private Integer shared;

    /** 可复制分享文案（M8 上线前给老人手动转发用） */
    private String shareText;
}