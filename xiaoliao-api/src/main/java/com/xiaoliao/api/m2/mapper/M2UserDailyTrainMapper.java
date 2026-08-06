package com.xiaoliao.api.m2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoliao.api.m2.entity.M2UserDailyTrain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

@Mapper
public interface M2UserDailyTrainMapper extends BaseMapper<M2UserDailyTrain> {

    @Select("""
            SELECT * FROM m2_user_daily_train
            WHERE user_id = #{userId}
              AND train_date = #{trainDate}
              AND deleted = 0
            LIMIT 1
            """)
    M2UserDailyTrain selectByUserAndDate(@Param("userId") Long userId, @Param("trainDate") LocalDate trainDate);

    @Insert("""
            INSERT INTO m2_user_daily_train
                (user_id, train_date, total_second, game_count, finish_round, abandon_round,
                 memory_score, attention_score, reason_score, language_score, balance_level)
            VALUES
                (#{userId}, #{trainDate}, #{totalSecond}, #{gameCount}, #{finishRound}, #{abandonRound},
                 #{memoryScore}, #{attentionScore}, #{reasonScore}, #{languageScore}, #{balanceLevel})
            ON CONFLICT (user_id, train_date) WHERE deleted = 0
            DO UPDATE SET
                total_second   = EXCLUDED.total_second,
                game_count     = EXCLUDED.game_count,
                finish_round   = EXCLUDED.finish_round,
                abandon_round  = EXCLUDED.abandon_round,
                memory_score   = EXCLUDED.memory_score,
                attention_score = EXCLUDED.attention_score,
                reason_score   = EXCLUDED.reason_score,
                language_score = EXCLUDED.language_score,
                balance_level  = EXCLUDED.balance_level,
                update_time    = CURRENT_TIMESTAMP
            """)
    int upsertDailyTrain(M2UserDailyTrain daily);
}
