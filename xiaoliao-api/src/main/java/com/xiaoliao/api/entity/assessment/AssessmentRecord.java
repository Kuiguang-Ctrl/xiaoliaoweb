package com.xiaoliao.api.entity.assessment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心理测评记录
 */
@Data
@TableName("assessment_records")
public class AssessmentRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    /** gds / sas */
    private String scaleType;

    private Integer score;

    private String result;

    private LocalDateTime createdAt;
}
