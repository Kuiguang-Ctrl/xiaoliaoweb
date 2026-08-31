package com.xiaoliao.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisLock 单元测试 —— 校验 SET NX EX 加锁语义、requestId 生成与 Lua 释放调用。
 */
@ExtendWith(MockitoExtension.class)
class RedisLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    void tryLock_成功时返回requestId并携带过期租约() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);

        RedisLock lock = new RedisLock(redisTemplate);
        String requestId = lock.tryLock("checkin:2026-08-27:user1", Duration.ofHours(30));

        assertNotNull(requestId);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).setIfAbsent(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
        assertEquals("checkin:2026-08-27:user1", keyCaptor.getValue());
        assertEquals(Duration.ofHours(30), ttlCaptor.getValue());
        assertEquals(requestId, valueCaptor.getValue());
    }

    @Test
    void tryLock_锁被占用时返回null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Boolean.FALSE);

        RedisLock lock = new RedisLock(redisTemplate);
        assertNull(lock.tryLock("checkin:2026-08-27:user1", Duration.ofSeconds(1)));
    }

    @Test
    void unlock_通过Lua脚本校验requestId后原子删除() {
        RedisLock lock = new RedisLock(redisTemplate);
        lock.unlock("checkin:2026-08-27:user1", "req-1");

        verify(redisTemplate).execute(any(DefaultRedisScript.class),
                eq(List.of("checkin:2026-08-27:user1")), eq("req-1"));
    }

    @Test
    void unlock_Redis异常时静默降级不抛出() {
        doThrow(new RuntimeException("redis down"))
                .when(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());

        RedisLock lock = new RedisLock(redisTemplate);
        assertDoesNotThrow(() -> lock.unlock("k", "req-1"));
    }
}
