package com.xiaoliao.api.config;

/**
 * 认证上下文 — 存放当前请求的用户标识
 */
public final class AuthContext {

    /** 当前登录用户的 request attribute key */
    public static final String USER_ID_ATTR = "xiaoliao.userId";

    private AuthContext() {
    }
}
