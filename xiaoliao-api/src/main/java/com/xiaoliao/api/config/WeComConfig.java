package com.xiaoliao.api.config;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信配置 — WxCpService Bean
 */
@Configuration
public class WeComConfig {

    @Value("${wecom.corp-id}")
    private String corpId;

    @Value("${wecom.secret}")
    private String secret;

    @Value("${wecom.token}")
    private String token;

    @Value("${wecom.aes-key}")
    private String aesKey;

    @Bean
    public WxCpService wxCpService() {
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(corpId);
        config.setCorpSecret(secret);
        config.setToken(token);
        config.setAesKey(aesKey);

        WxCpServiceImpl service = new WxCpServiceImpl();
        service.setWxCpConfigStorage(config);
        return service;
    }
}
