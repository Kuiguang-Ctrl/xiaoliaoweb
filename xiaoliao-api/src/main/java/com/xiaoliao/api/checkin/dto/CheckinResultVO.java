package com.xiaoliao.api.checkin.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 快捷签到响应
 */
@Data
@Builder
public class CheckinResultVO {

    /** 签到日期 yyyy-MM-dd */
    private String checkinDate;

    /** 本次签到情绪，未选为 null */
    private String mood;

    /** 连续签到天数（含本次） */
    private Integer consecutiveDays;

    /** 即时正向反馈语（前端可播报/动画） */
    private String feedback;

    /** 是否触发"连续 3 天负面情绪"关怀 */
    private boolean careAlert;
}
