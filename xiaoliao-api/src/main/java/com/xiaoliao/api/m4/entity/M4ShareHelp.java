package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("m4_share_help")
public class M4ShareHelp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private Long nodeId;

    private String shareToken;

    /** 亲友补的文字/提示（Service 层 AES-GCM 加密落库） */
    private String helperNote;

    /** 亲友补的照片URL */
    private String helperPhotoUrl;

    private LocalDateTime expiredAt;

    /** 0待补 1老人已确认 2已作废 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
