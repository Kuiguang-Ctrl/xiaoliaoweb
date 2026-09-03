package com.xiaoliao.api.chat.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.chat.entity.ProactiveMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 主动消息 Mapper
 */
@Mapper
public interface ProactiveMessageMapper extends BaseMapper<ProactiveMessage> {
}