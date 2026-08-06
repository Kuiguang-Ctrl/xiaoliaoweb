package com.xiaoliao.api.auth;

import com.xiaoliao.api.auth.dto.VerifyTokenRequest;
import com.xiaoliao.api.auth.dto.VerifyTokenResponse;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.api.user.UserService;
import com.xiaoliao.api.util.TokenUtil;
import com.xiaoliao.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口 — 小程序登录验证
 * <p>
 * 小程序从企微卡片链接中拿到 JWT token，调此接口换取 userId。
 * 老人全程无感，不需要输入手机号/验证码。
 */
@Tag(name = "认证")
@Slf4j
@Tag(name = "认证接口", description = "小程序登录验证、JWT token 校验")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenUtil tokenUtil;
    private final UserService userService;

    /**
     * 验证 JWT token，返回用户信息
     * <p>
     * 小程序打开时调用：POST /api/auth/verify-token { "token": "xxx" }
     */
    @Operation(summary = "验证登录令牌", description = "小程序从企微卡片链接拿到 JWT token，调此接口换取 userId")
    @PostMapping("/verify-token")
    public Result<VerifyTokenResponse> verifyToken(@Valid @RequestBody VerifyTokenRequest request) {
        String userId = tokenUtil.parseUserId(request.getToken());

        if (userId == null) {
            return Result.ok(VerifyTokenResponse.builder()
                    .valid(false)
                    .build());
        }

        User user = userService.getById(userId);
        if (user == null) {
            log.warn("JWT 有效但用户不存在: userId={}", userId);
            return Result.ok(VerifyTokenResponse.builder()
                    .valid(false)
                    .build());
        }

        return Result.ok(VerifyTokenResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .valid(true)
                .build());
    }
}
