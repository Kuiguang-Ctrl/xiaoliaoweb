package com.xiaoliao.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 小程序登录验证请求
 */
@Data
public class VerifyTokenRequest {

    @NotBlank(message = "token 不能为空")
    private String token;
}
