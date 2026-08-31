package com.xiaoliao.api.m4.vo;

import lombok.Data;

/**
 * 隐私授权 VO
 */
@Data
public class ConsentVO {

    private Long id;

    /** 授权类型：cloud_storage=云端存储 anonymous_stats=匿名统计 */
    private String consentType;

    /** 0未授权 1已授权 */
    private Integer granted;

    private String updateTime;
}