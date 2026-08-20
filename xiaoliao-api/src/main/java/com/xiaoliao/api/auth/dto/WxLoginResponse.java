package com.xiaoliao.api.auth.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 微信小程序登录响应 — wx.login 登录成功后返回
 */
@Data
@Builder
public class WxLoginResponse {

    /** JWT 令牌，后续所有接口携带 Authorization: Bearer <token> */
    private String token;
    private String userId;
    private String nickname;
}
