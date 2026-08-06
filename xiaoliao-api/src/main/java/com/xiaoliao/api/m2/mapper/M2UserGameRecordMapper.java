package com.xiaoliao.api.m2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.m2.entity.M2UserGameRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户单局游戏记录 Mapper
 */
@Mapper
public interface M2UserGameRecordMapper extends BaseMapper<M2UserGameRecord> {

    /**
     * 取用户某游戏最近一局已完成记录（用于延续难度）
     */
    @Select("""
            SELECT * FROM m2_user_game_record
            WHERE user_id = #{userId}
              AND game_id = #{gameId}
              AND game_status = 1
              AND deleted = 0
            ORDER BY end_time DESC
            LIMIT 1
            """)
    M2UserGameRecord selectLatestFinished(@Param("userId") Long userId, @Param("gameId") Long gameId);
}
