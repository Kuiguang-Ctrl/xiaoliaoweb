package com.xiaoliao.api.conversation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.conversation.entity.ConversationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话日志 Mapper
 */
@Mapper
public interface ConversationLogMapper extends BaseMapper<ConversationLog> {
}
