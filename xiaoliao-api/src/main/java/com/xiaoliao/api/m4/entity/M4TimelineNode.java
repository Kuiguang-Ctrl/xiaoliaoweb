package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_timeline_node")
public class M4TimelineNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 预设阶段：childhood/school/work/marriage/parenting/grandchildren/retirement */
    private String stage;

    /** 自定义阶段名（老人改过时优先展示） */
    private String customName;

    private Integer sortOrder;

    private Integer yearFrom;

    private Integer yearTo;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
