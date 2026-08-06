package com.xiaoliao.api.checkin.dto;

import lombok.Data;

/**
 * 快捷签到请求
 * <p>
 * mood 可空：老人点情绪按钮就传，纯签到不传（null）。
 */
@Data
public class CheckinRequest {

    /** 情绪：sunny/cloudy/overcast/rain/storm，可空 */
    private String mood;

    /** 情绪备注，可空 */
    private String moodNote;
}
