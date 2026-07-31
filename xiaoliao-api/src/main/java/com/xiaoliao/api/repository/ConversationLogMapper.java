package com.xiaoliao.api.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.entity.ConversationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话日志 Mapper
 */
@Mapper
public interface ConversationLogMapper extends BaseMapper<ConversationLog> {
}
