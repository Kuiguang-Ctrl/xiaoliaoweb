package com.xiaoliao.api.exercise.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.exercise.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心理练习记录 Mapper
 */
@Mapper
public interface ExerciseMapper extends BaseMapper<ExerciseRecord> {
}
