package com.xiaoliao.api.entity.checkin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到与情绪记录
 */
@Data
@TableName("checkin_records")
public class CheckinRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    /** 情绪: sunny/cloudy/rainy */
    private String mood;

    /** 情绪备注 */
    private String moodNote;

    private LocalDate checkinDate;

    private LocalDateTime createdAt;
}
