package com.xiaoliao.api.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.api.user.repository.UserMapper;
import com.xiaoliao.api.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务 — 企微用户自动注册 + JWT 令牌管理
 * <p>
 * 老人加企微客服发第一条消息时，自动在 users 表建记录，全程无感"登录"。
 * 后续小程序卡片携带 JWT，小程序打开后调 /auth/verify-token 换 userId。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final TokenUtil tokenUtil;

    /**
     * 根据企微 external_userid 查询或创建用户
     *
     * @param externalUserId 企微回调里的 external_userid
     * @return 小辽系统内的 User 实体
     */
    public User getOrCreateUser(String externalUserId) {
        if (externalUserId == null || externalUserId.isBlank()) {
            throw new IllegalArgumentException("externalUserId 不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, externalUserId)
                        .last("LIMIT 1")
        );

        if (user != null) {
            return user;
        }

        // 首次消息，自动注册
        user = new User();
        user.setOpenid(externalUserId);
        user.setNickname("小辽朋友");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        log.info("新用户自动注册: openid={}, userId={}", externalUserId, user.getId());
        return user;
    }

    /**
     * 根据 userId 查询用户
     */
    public User getById(String userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 为用户生成 JWT 令牌（供小程序卡片链接使用）
     */
    public String generateToken(String userId) {
        return tokenUtil.generateToken(userId);
    }
}
