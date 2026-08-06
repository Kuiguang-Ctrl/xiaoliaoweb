package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日训练汇总表
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_user_daily_train")
public class M2UserDailyTrain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate trainDate;

    /** 当日总时长（秒） */
    private Integer totalSecond;

    /** 游玩游戏种类数 */
    private Integer gameCount;

    /** 完成局数 */
    private Integer finishRound;

    /** 放弃局数 */
    private Integer abandonRound;

    /** 记忆均分 0-100 */
    private Integer memoryScore;

    /** 注意力均分 */
    private Integer attentionScore;

    /** 推理均分 */
    private Integer reasonScore;

    /** 语言均分 */
    private Integer languageScore;

    /** 训练均衡度 0-100 */
    private Integer balanceLevel;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
