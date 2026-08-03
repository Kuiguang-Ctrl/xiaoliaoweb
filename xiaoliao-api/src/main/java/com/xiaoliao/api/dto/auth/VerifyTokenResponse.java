package com.xiaoliao.api.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 小程序登录验证响应
 */
@Data
@Builder
public class VerifyTokenResponse {

    private String userId;
    private String nickname;
    private boolean valid;
}
