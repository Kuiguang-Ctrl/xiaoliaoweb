package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场点赞（一人一作品一条，取消点赞即物理删除）
 */
@Data
@TableName("m4_plaza_like")
public class M4PlazaLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workId;

    private String userId;

    private LocalDateTime createTime;
}
