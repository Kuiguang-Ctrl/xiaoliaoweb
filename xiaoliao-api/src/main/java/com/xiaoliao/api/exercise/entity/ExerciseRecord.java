package com.xiaoliao.api.exercise.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积极心理练习记录
 */
@Data
@TableName("exercise_records")
public class ExerciseRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    /** three_good_things / gratitude / reframing */
    private String exerciseType;

    private String content;

    private LocalDateTime createdAt;
}
