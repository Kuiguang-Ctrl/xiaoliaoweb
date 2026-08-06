package com.xiaoliao.api.checkin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.checkin.entity.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录 Mapper
 */
@Mapper
public interface CheckinMapper extends BaseMapper<CheckinRecord> {
}
