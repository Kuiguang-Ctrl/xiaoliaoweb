package com.xiaoliao.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoliao.api.util.TokenUtil;
import com.xiaoliao.common.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器 — 从 Authorization: Bearer <token> 解析 userId
 * <p>
 * 通过后把 userId 放进 request attribute，Controller 用
 * {@code @RequestAttribute(AuthContext.USER_ID_ATTR)} 获取。
 * <p>
 * 可通过 {@code xiaoliao.auth.enabled=false} 跳过鉴权（联调用，配合 mock-user-id）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenUtil tokenUtil;
    private final ObjectMapper objectMapper;

    /** 鉴权开关：true=校验 token；false=跳过（联调用） */
    @Value("${xiaoliao.auth.enabled:true}")
    private boolean authEnabled;

    /** 跳过鉴权时使用的固定用户 id */
    @Value("${xiaoliao.auth.mock-user-id:}")
    private String mockUserId;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String header = request.getHeader("Authorization");
        log.info("鉴权请求: uri={}, authHeader=[{}], authEnabled={}", request.getRequestURI(), header, authEnabled);
        String headerUserId = (header == null || header.isBlank()) ? null : tokenUtil.parseUserId(header);

        String userId;
        if (authEnabled) {
            // 正式校验
            userId = headerUserId;
            if (userId == null) {
                log.warn("未认证请求: uri={}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json;charset=UTF-8");
                objectMapper.writeValue(response.getWriter(), Result.fail(401, "登录已失效，请重新进入"));
                return false;
            }
        } else {
            // 联调跳过鉴权：优先用请求头里的 token，没有则用 mock 用户
            userId = (headerUserId != null) ? headerUserId : mockUserId;
            log.warn("鉴权已关闭(dev)，userId={}", userId);
        }

        request.setAttribute(AuthContext.USER_ID_ATTR, userId);
        return true;
    }
}
