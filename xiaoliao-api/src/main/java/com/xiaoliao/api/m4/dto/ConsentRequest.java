package com.xiaoliao.api.m4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 隐私授权请求
 */
@Data
public class ConsentRequest {

    /** 授权类型：cloud_storage=云端存储 anonymous_stats=匿名统计 */
    @NotBlank(message = "授权类型不能为空")
    @Size(max = 32, message = "授权类型不合法")
    private String consentType;

    /** 0未授权 1已授权 */
    @NotNull(message = "授权状态不能为空")
    private Integer granted;
}