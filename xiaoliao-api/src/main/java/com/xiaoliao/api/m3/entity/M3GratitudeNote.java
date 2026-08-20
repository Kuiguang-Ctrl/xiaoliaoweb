package com.xiaoliao.api.m3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M3 感恩留言
 */
@Data
@TableName("m3_gratitude_notes")
public class M3GratitudeNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 感谢对象：老伴/女儿/儿子/老友/自定义 */
    private String targetName;

    /** 留言内容 */
    private String content;

    /** 语音留言 URL（本期预留） */
    private String voiceUrl;

    /** 是否已分享给家人 0未分享 1已分享（M8 接入） */
    private Integer shared;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}