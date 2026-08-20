package com.xiaoliao.api.m3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * M3 三件好事记录
 */
@Data
@TableName("m3_good_thing_records")
public class M3GoodThingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 好事内容（纯文本，语音转文字二期接入） */
    private String content;

    /** 记录日期 */
    private LocalDate recordDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}