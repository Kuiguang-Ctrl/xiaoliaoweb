package com.xiaoliao.api.checkin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 今日状态（小程序首页）
 */
@Data
@Builder
public class TodayVO {

    /** 今天日期 yyyy-MM-dd */
    private String date;

    /** 星期（中文，如"星期二"） */
    private String weekday;

    /** 季节：春/夏/秋/冬 */
    private String season;

    /** 问候语 */
    private String greeting;

    /** 天气描述，预留字段，暂为 null */
    private String weather;

    /** 今日是否已签到 */
    private boolean checkedIn;

    /** 今日情绪，已签到但没选情绪或未签到均为 null */
    private String todayMood;

    /** 连续签到天数（未签到时为今日之前的连续数） */
    private Integer consecutiveDays;
}
