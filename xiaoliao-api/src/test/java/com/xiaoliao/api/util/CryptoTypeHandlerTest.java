package com.xiaoliao.api.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CryptoTypeHandler 单元测试 —— AES-256-GCM 敏感字段加解密与旧数据兼容。
 */
class CryptoTypeHandlerTest {

    @BeforeEach
    void setUp() {
        CryptoTypeHandler.setKey("unit-test-secret-2026");
    }

    @Test
    void encryptThenDecrypt_往返一致() {
        String plain = "用户手机号 13800138000";
        String stored = CryptoTypeHandler.encrypt(plain);

        assertTrue(stored.startsWith("enc:"), "密文应带 enc: 前缀标记");
        assertNotEquals(plain, stored, "密文不应等于明文");
        assertEquals(plain, CryptoTypeHandler.decrypt(stored));
    }

    @Test
    void encrypt_随机IV使同一明文两次结果不同() {
        String first = CryptoTypeHandler.encrypt("同一条敏感数据");
        String second = CryptoTypeHandler.encrypt("同一条敏感数据");

        assertNotEquals(first, second, "每次加密应使用随机 IV，密文不可预测");
        assertEquals("同一条敏感数据", CryptoTypeHandler.decrypt(first));
        assertEquals("同一条敏感数据", CryptoTypeHandler.decrypt(second));
    }

    @Test
    void decrypt_无enc前缀旧数据原样返回() {
        assertEquals("legacy-plaintext", CryptoTypeHandler.decrypt("legacy-plaintext"));
    }

    @Test
    void null_与空值透传() {
        assertNull(CryptoTypeHandler.encrypt(null));
        assertNull(CryptoTypeHandler.decrypt(null));
    }
}
