package com.xiaoliao.api.repository.game;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.entity.game.GameRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏记录 Mapper
 */
@Mapper
public interface GameMapper extends BaseMapper<GameRecord> {
}
