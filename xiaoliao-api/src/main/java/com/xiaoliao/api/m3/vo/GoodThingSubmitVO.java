package com.xiaoliao.api.m3.vo;

import lombok.Data;

/**
 * 提交好事结果
 */
@Data
public class GoodThingSubmitVO {

    private Long id;

    private String content;

    /** yyyy-MM-dd */
    private String recordDate;

    /** 今日已记录条数（含本条） */
    private int todayCount;

    /** 鼓励语 */
    private String encourageMsg;
}