package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场作品（老人从自己的视频作品里发布出来的）
 */
@Data
@TableName("m4_plaza_work")
public class M4PlazaWork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 来源 m4_video.id */
    private Long videoId;

    /** 作品标题（发布时快照） */
    private String title;

    /** 视频地址 /uploads/videos/... */
    private String videoUrl;

    /** 封面图（视频首帧） */
    private String coverUrl;

    /** 时长秒 */
    private Integer duration;

    /** 点赞数（冗余计数） */
    private Integer likeCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
