package com.xiaoliao.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TokenUtil 单元测试 —— JWT 生成/解析、Bearer 前缀与空白容错、无效令牌兜底。
 */
class TokenUtilTest {

    private TokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        tokenUtil = new TokenUtil();
        ReflectionTestUtils.setField(tokenUtil, "secret", "unit-test-secret-0123456789-0123456789");
        ReflectionTestUtils.setField(tokenUtil, "expireHours", 168);
        tokenUtil.init();
    }

    @Test
    void generateThenParse_往返一致() {
        String token = tokenUtil.generateToken("external_user_123");

        assertNotNull(token);
        assertEquals("external_user_123", tokenUtil.parseUserId(token));
        assertTrue(tokenUtil.isValid(token));
    }

    @Test
    void parseUserId_兼容Bearer前缀与周边空白() {
        String token = tokenUtil.generateToken("u-1");

        assertEquals("u-1", tokenUtil.parseUserId("Bearer " + token));
        assertEquals("u-1", tokenUtil.parseUserId("  bearer  " + token + " \n"));
    }

    @Test
    void parseUserId_无效或空令牌返回null() {
        assertNull(tokenUtil.parseUserId("not-a-jwt"));
        assertNull(tokenUtil.parseUserId(""));
        assertNull(tokenUtil.parseUserId(null));
    }
}
