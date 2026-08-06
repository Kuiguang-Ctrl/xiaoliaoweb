package com.xiaoliao.api.m2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏素材表
 *
 * @author m2
 * @since 2026-08-03
 */
@Data
@TableName("m2_game_resource")
public class M2GameResource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属游戏 id */
    private Long gameId;

    /** 素材类型：图片 / 文字 / 数字 */
    private String resType;

    /** 图片地址 / 成语 / 数字 */
    private String resContent;

    /** 素材标签：怀旧、动物等 */
    private String resTag;

    /** 是否启用 1/0 */
    private Integer isEnable;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
