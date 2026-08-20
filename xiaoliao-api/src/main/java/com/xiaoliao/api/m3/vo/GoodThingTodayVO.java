package com.xiaoliao.api.m3.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 今日三件好事
 */
@Data
public class GoodThingTodayVO {

    private List<GoodThingRecordVO> records = new ArrayList<>();

    /** 今日已记录条数 */
    private int todayCount;

    /** 每日上限 */
    private int dailyLimit = 3;
}