package com.xiaoliao.api.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis 的轻量级分布式锁
 * <p>
 * 加锁：SET key requestId NX EX，一条命令原子完成「不存在才设置 + 过期时间」；
 * 释放：Lua 脚本先比对 requestId 再删除，防止误删他人刚获取的锁。
 * 过期时间即租约，业务超时后自动释放，避免死锁。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /**
     * 尝试加锁
     *
     * @return 成功返回 requestId（释放锁时校验用）；锁已被占用返回 null；Redis 异常向上抛出，由调用方决定是否降级
     */
    public String tryLock(String key, Duration timeout) {
        String requestId = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, requestId, timeout);
        return Boolean.TRUE.equals(locked) ? requestId : null;
    }

    /**
     * Lua 原子释放：仅当锁的 value 仍为本请求的 requestId 时才删除，防止误删他人刚获取的锁
     */
    public void unlock(String key, String requestId) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), requestId);
        } catch (Exception e) {
            log.warn("释放分布式锁失败，等待过期自动释放: key={}, err={}", key, e.getMessage());
        }
    }
}
