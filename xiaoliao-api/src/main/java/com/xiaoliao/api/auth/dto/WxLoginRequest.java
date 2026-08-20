package com.xiaoliao.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求 — wx.login() 拿到的临时 code
 */
@Data
public class WxLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;
}
