package com.xiaoliao.api.repository.checkin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.entity.checkin.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录 Mapper
 */
@Mapper
public interface CheckinMapper extends BaseMapper<CheckinRecord> {
}
