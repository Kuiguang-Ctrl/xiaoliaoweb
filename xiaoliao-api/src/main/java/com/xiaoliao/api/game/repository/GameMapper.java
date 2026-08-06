package com.xiaoliao.api.game.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.game.entity.GameRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏记录 Mapper
 */
@Mapper
public interface GameMapper extends BaseMapper<GameRecord> {
}
