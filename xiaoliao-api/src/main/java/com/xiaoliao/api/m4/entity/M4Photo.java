package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_photo")
public class M4Photo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** user=老人上传 stock=系统示意图 */
    private String source;

    private String url;

    /** 是否年代示意图 0否 1是 */
    private Integer isIllustration;

    private String eraTag;

    private Long nodeId;

    private Long eraId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
