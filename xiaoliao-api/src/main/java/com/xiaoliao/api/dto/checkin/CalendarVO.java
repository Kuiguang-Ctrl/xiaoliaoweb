package com.xiaoliao.api.dto.checkin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 情绪月历响应
 */
@Data
@Builder
public class CalendarVO {

    /** 查询月份 yyyy-MM */
    private String month;

    /** 当月每日签到记录（按日期升序） */
    private List<DayVO> checkins;

    /** 月度统计 */
    private CalendarStatsVO stats;

    @Data
    @Builder
    public static class DayVO {
        /** 签到日期 yyyy-MM-dd */
        private String date;

        /** 当天情绪，纯签到没选情绪为 null */
        private String mood;

        /** 当天备注 */
        private String moodNote;
    }

    @Data
    @Builder
    public static class CalendarStatsVO {
        /** 本月签到天数 */
        private int totalDays;

        /** 正面情绪天数（sunny/cloudy） */
        private int positiveDays;

        /** 负面情绪天数（rain/storm） */
        private int negativeDays;

        /** 纯签到无情绪天数（mood 为 null） */
        private int noMoodDays;
    }
}
