package com.xiaoliao.api.m3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M3 成就墙收藏（跨模块聚合）
 */
@Data
@TableName("m3_achievement_collect")
public class M3AchievementCollect {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 成就来源类型：game=游戏成就，milestone=动态里程碑 */
    private String sourceType;

    /** 来源标识：game 指向 m2_user_game_achievement.id；milestone 为徽章标识 */
    private String sourceId;

    private LocalDateTime createTime;
}