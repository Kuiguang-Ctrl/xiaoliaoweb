package com.xiaoliao.api.m3.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 每周开心小结
 */
@Data
public class WeeklySummaryVO {

    /** yyyy-MM-dd 周一 */
    private String weekStart;

    /** yyyy-MM-dd 周日 */
    private String weekEnd;

    private long goodThingCount;

    private long gratitudeCount;

    private long gameRoundCount;

    private long achievementCount;

    private long checkinDays;

    /** 模板生成的小结文案 */
    private String summary;

    private String encourageMsg;

    private List<String> tips = new ArrayList<>();
}