package com.xiaoliao.api.auth;

import com.xiaoliao.api.auth.dto.VerifyTokenRequest;
import com.xiaoliao.api.auth.dto.VerifyTokenResponse;
import com.xiaoliao.api.auth.dto.WxLoginRequest;
import com.xiaoliao.api.auth.dto.WxLoginResponse;
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
    private final WxAuthService wxAuthService;

    /**
     * 微信小程序无感登录（直开小程序时调用）
     * <p>
     * 小程序 wx.login() 拿到临时 code → 换 openid → 查/建用户 → 返回 JWT。
     * 全程无感，老人直接打开就能用。
     */
    @Operation(summary = "微信无感登录", description = "小程序 wx.login() 拿 code 换 openid，自动注册并返回 JWT")
    @PostMapping("/wx-login")
    public Result<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        String openid = wxAuthService.codeToOpenid(request.getCode());
        if (openid == null) {
            log.warn("wx-login 换 openid 失败: code={}", request.getCode());
            return Result.fail(401, "微信登录失败，请重试");
        }

        User user = userService.getOrCreateByWechatOpenid(openid);
        String token = tokenUtil.generateToken(user.getId());

        log.info("微信无感登录成功: userId={}, nickname={}", user.getId(), user.getNickname());
        return Result.ok(WxLoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .build());
    }

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
