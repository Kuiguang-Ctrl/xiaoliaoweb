package com.xiaoliao.api.entity.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脑力游戏记录
 */
@Data
@TableName("game_records")
public class GameRecord {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String userId;

    /** number_memory / color_reaction / word_match / spatial */
    private String gameType;

    private Integer score;

    private Integer durationSeconds;

    /** 游戏详细数据 (JSONB) */
    private String details;

    private LocalDateTime createdAt;
}
