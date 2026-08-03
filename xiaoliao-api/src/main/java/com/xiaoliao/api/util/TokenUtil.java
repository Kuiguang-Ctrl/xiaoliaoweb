package com.xiaoliao.api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具 — 为小程序生成/验证登录令牌
 * <p>
 * 流程：企微首次消息 → 自动注册 → 生成 JWT → 小程序卡片链接携带 → 小程序调 /auth/verify-token 换 userId
 */
@Slf4j
@Component
public class TokenUtil {

    @Value("${xiaoliao.jwt.secret}")
    private String secret;

    @Value("${xiaoliao.jwt.expire-hours:168}")
    private int expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT（默认 7 天过期）
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) expireHours * 3600_000);
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT，返回 userId
     *
     * @return userId，token 无效或过期返回 null
     */
    public String parseUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        // 去掉 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 JWT 是否有效
     */
    public boolean isValid(String token) {
        return parseUserId(token) != null;
    }
}
