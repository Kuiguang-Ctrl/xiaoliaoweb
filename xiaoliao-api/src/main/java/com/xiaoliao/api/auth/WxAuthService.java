package com.xiaoliao.api.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 微信小程序登录服务 — wx.login() code 换 openid
 * <p>
 * 调微信官方 jscode2session 接口，用 AppID + AppSecret + code 换 openid。
 * openid 是微信侧唯一用户标识，后续用它查/建 users 表记录。
 */
@Slf4j
@Service
public class WxAuthService {

    /** 微信 jscode2session 接口 */
    private static final String WX_CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    @Value("${xiaoliao.wx.appid:}")
    private String appid;

    @Value("${xiaoliao.wx.secret:}")
    private String secret;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxAuthService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 用 wx.login 的 code 换取 openid
     *
     * @param code wx.login() 返回的临时凭证
     * @return openid，换取失败返回 null
     */
    public String codeToOpenid(String code) {
        if (appid == null || appid.isBlank() || secret == null || secret.isBlank()) {
            log.warn("微信登录未配置 AppID/AppSecret");
            return null;
        }

        try {
            // 微信接口返回的 content-type 是 text/plain，先按字符串接收再解析 JSON
            String raw = restClient.get()
                    .uri(WX_CODE2SESSION_URL, appid, secret, code)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> body = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });

            // 微信返回错误：{errcode: 40029, errmsg: "invalid code"}
            Integer errcode = (Integer) body.get("errcode");
            if (errcode != null && errcode != 0) {
                log.warn("微信 jscode2session 失败: errcode={}, errmsg={}",
                        errcode, body.get("errmsg"));
                return null;
            }

            String openid = (String) body.get("openid");
            if (openid == null || openid.isBlank()) {
                log.warn("微信 jscode2session 未返回 openid");
                return null;
            }
            return openid;
        } catch (Exception e) {
            log.error("调用微信 jscode2session 异常", e);
            return null;
        }
    }
}