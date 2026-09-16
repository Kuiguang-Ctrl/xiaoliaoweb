package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_video")
public class M4Video {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 所属人生时光节点（可选） */
    private Long nodeId;

    /** 所属年代记忆（可选） */
    private Long eraId;

    /** 作品标题 */
    private String title;

    /** 完整文案（详情展示） */
    private String caption;

    /** 视频地址 /uploads/videos/... */
    private String videoUrl;

    /** 封面帧地址 */
    private String posterUrl;

    /** 时长秒 */
    private Integer duration;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
