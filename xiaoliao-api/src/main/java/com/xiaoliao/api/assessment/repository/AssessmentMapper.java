package com.xiaoliao.api.assessment.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.assessment.entity.AssessmentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心理测评记录 Mapper
 */
@Mapper
public interface AssessmentMapper extends BaseMapper<AssessmentRecord> {
}
