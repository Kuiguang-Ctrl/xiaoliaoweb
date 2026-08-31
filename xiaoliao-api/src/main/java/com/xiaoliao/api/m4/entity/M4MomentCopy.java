package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_moment_copy")
public class M4MomentCopy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private Long photoId;

    private Long storyId;

    private String content;

    /** 风格：simple/warm/humorous/proud */
    private String style;

    /** 是否被老人选中 0否 1是 */
    private Integer selected;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
