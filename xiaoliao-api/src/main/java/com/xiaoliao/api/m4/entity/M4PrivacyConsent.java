package com.xiaoliao.api.m4.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M4 隐私授权记录
 */
@Data
@TableName("m4_privacy_consent")
public class M4PrivacyConsent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    /** 授权类型：cloud_storage=云端存储 anonymous_stats=匿名统计 */
    private String consentType;

    /** 0未授权 1已授权 */
    private Integer granted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}