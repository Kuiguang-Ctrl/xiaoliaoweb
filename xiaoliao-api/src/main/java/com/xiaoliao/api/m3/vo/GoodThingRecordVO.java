package com.xiaoliao.api.m3.vo;

import lombok.Data;

/**
 * 好事记录
 */
@Data
public class GoodThingRecordVO {

    private Long id;

    private String content;

    /** yyyy-MM-dd */
    private String recordDate;

    /** yyyy-MM-dd HH:mm */
    private String createTime;
}