package com.xiaoliao.api.repository.exercise;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.entity.exercise.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心理练习记录 Mapper
 */
@Mapper
public interface ExerciseMapper extends BaseMapper<ExerciseRecord> {
}
