package com.xiaoliao.api.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.entity.Lesson;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教训库 Mapper
 */
@Mapper
public interface LessonMapper extends BaseMapper<Lesson> {
}
