package com.xiaoliao.api.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoliao.api.user.entity.User;
import com.xiaoliao.api.user.repository.UserMapper;
import com.xiaoliao.api.util.TokenUtil;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.external.contact.ExternalContact;
import me.chanjar.weixin.cp.bean.kf.WxCpKfCustomerBatchGetResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private static final String DEFAULT_NICKNAME = "小辽朋友";

    private final WxCpService wxCpService;
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
            // 每次客户发消息都刷新活跃时间（供每日定时提醒筛选 48 小时窗口）
            user.setLastActiveAt(LocalDateTime.now());
            // 昵称还是默认占位时，尝试补一次企微真实昵称（避免每次消息都调接口）
            if (DEFAULT_NICKNAME.equals(user.getNickname())) {
                refreshNickname(user, externalUserId);
            }
            userMapper.updateById(user);
            return user;
        }

        // 首次消息，自动注册
        user = new User();
        user.setOpenid(externalUserId);
        user.setNickname("小辽朋友");
        user.setLastActiveAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        // 建号后立即用企微客户详情补真实昵称
        refreshNickname(user, externalUserId);

        log.info("新用户自动注册: openid={}, userId={}", externalUserId, user.getId());
        return user;
    }

    /**
     * 根据微信 openid 查询或创建用户（wx.login 登录用）
     * <p>
     * 与企微 external_userid 共用 openid 字段：两者都是微信体系内的用户标识，
     * 各自格式不同（企微 external_userid 形如 wmXXX，微信 openid 形如 oXXX），不会冲突。
     *
     * @param wechatOpenid wx.login 换到的 openid
     * @return 小辽系统内的 User 实体
     */
    public User getOrCreateByWechatOpenid(String wechatOpenid) {
        if (wechatOpenid == null || wechatOpenid.isBlank()) {
            throw new IllegalArgumentException("openid 不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getOpenid, wechatOpenid)
                        .last("LIMIT 1")
        );

        if (user != null) {
            return user;
        }

        // 首次打开小程序，自动注册
        user = new User();
        user.setOpenid(wechatOpenid);
        user.setNickname("小辽朋友");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        log.info("微信用户自动注册: openid={}, userId={}", wechatOpenid, user.getId());
        return user;
    }

    /**
     * 根据 userId 查询用户
     */
    public User getById(String userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 批量取昵称（广场作品作者名、亲友协作等展示用）
     * <p>昵称为空或仍是默认占位时，返回 {@code 小辽朋友}，不把空名字透给前端。
     *
     * @param userIds 用户 id 集合
     * @return userId → 展示名
     */
    public Map<String, String> nicknameMap(Collection<String> userIds) {
        Map<String, String> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        for (User u : userMapper.selectBatchIds(userIds)) {
            String n = u.getNickname();
            map.put(u.getId(), (n == null || n.isBlank()) ? DEFAULT_NICKNAME : n);
        }
        return map;
    }

    /**
     * 确保 userId 在 users 表存在（联调 mock/多测试用户：清库后首个请求自动补建，避免 401）
     */
    public User ensureById(String userId) {
        User u = userMapper.selectById(userId);
        if (u != null) {
            return u;
        }
        u = new User();
        u.setId(userId);
        u.setOpenid("mock_" + userId);
        u.setNickname("测试用户");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        u.setLastActiveAt(LocalDateTime.now());
        try {
            userMapper.insert(u);
            log.info("mock 测试用户自动建号: userId={}", userId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发首请求都来建号：已存在则直接复用
            u = userMapper.selectById(userId);
        }
        return u;
    }

    /**
     * 为用户生成 JWT 令牌（供小程序卡片链接使用）
     */
    public String generateToken(String userId) {
        return tokenUtil.generateToken(userId);
    }
    /**
     * 用企微「获取客户详情」接口补客户微信昵称（customerBatchGet）
     * 失败不影响主流程，保留默认昵称
     */
    private void refreshNickname(User user, String externalUserId) {
        try {
            WxCpKfCustomerBatchGetResp resp = wxCpService.getKfService()
                    .customerBatchGet(Collections.singletonList(externalUserId));
            if (resp != null && resp.getCustomerList() != null && !resp.getCustomerList().isEmpty()) {
                ExternalContact customer = resp.getCustomerList().get(0);
                String nickname = customer.getNickname();
                if (nickname != null && !nickname.isBlank()) {
                    user.setNickname(nickname);
                    user.setUpdatedAt(LocalDateTime.now());
                    userMapper.updateById(user);
                    log.info("已回填客户昵称: externalUserId={}, nickname={}", externalUserId, nickname);
                }
            }
        } catch (Exception e) {
            log.warn("获取客户详情失败，保留默认昵称: externalUserId={}, err={}", externalUserId, e.getMessage());
        }
    }
}
