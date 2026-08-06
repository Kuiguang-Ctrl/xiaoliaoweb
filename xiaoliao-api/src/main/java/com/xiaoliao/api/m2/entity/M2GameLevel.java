package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自适应难度配置表
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_game_level")
public class M2GameLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 m2_game_info.id */
    private Long gameId;

    /** 难度序号 1-4 */
    private Integer levelNo;

    /** 难度名称 */
    private String levelName;

    /**
     * 难度配置 JSON（行列、题目数、重复间隔等）
     * 示例：{"rows":2,"cols":2,"questionCount":8,"repeatInterval":3}
     */
    private String configJson;

    /** 升级阈值，默认 80 */
    private Integer passRateUp;

    /** 降级阈值，默认 40 */
    private Integer passRateDown;

    /** 难度排序 */
    private Integer sort;

    /** 是否启用 1/0 */
    private Integer isEnable;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
