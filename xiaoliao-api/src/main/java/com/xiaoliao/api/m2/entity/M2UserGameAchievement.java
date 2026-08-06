package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏成就表
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_user_game_achievement")
public class M2UserGameAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联单局记录 id */
    private Long recordId;

    private Long gameId;

    /** 本局星级 */
    private Integer star;

    /** 成就文案 */
    private String achievementDesc;

    /** 0 未分享 1 已分享社区 */
    private Integer shareStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
