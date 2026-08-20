package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户单局游戏记录表（自适应核心）
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_user_game_record")
public class M2UserGameRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private Long gameId;

    /** 本局难度 id */
    private Long levelId;

    /** 开局难度序号 */
    private Integer preLevelNo;

    /** 自适应后下局难度序号 */
    private Integer nextLevelNo;

    /** 认知域 */
    private String cognitiveType;

    private LocalDate playDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 耗时秒 */
    private Integer costSecond;

    private Integer totalQuestion;

    private Integer correctQuestion;

    private Integer wrongQuestion;

    /** 本局正确率 0-100 */
    private Integer passRate;

    /** 获得星级 0-3 */
    private Integer star;

    /** 0 进行中 1 完成 2 放弃 */
    private Integer gameStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
