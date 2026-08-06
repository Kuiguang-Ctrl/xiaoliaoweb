package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏基础信息表
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_game_info")
public class M2GameInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 游戏编码：memory / attention / reason / language */
    private String gameCode;

    /** 游戏名称 */
    private String gameName;

    /** 认知域：记忆 / 注意力 / 推理 / 语言 */
    private String cognitiveDomain;

    /** 游戏简介 */
    private String gameDesc;

    /** 标准单局时长（分钟） */
    private Integer singleMinute;

    /** 单局最大时长（分钟） */
    private Integer maxMinute;

    /** 0 不扣分（无错学习）、1 扣分 */
    private Integer errorPunish;

    /** 单局最高星级 */
    private Integer starTotal;

    /** 展示排序 */
    private Integer sort;

    /** 1 启用 0 下架 */
    private Integer isEnable;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
