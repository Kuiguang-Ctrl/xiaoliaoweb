package com.xiaoliao.api.config;

import com.xiaoliao.api.util.CryptoTypeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动时把数据加密密钥注入 CryptoTypeHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeyInitializer implements ApplicationRunner {

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        String key = environment.getProperty("xiaoliao.security.data-key");
        CryptoTypeHandler.setKey(key);
        log.info("数据加密密钥已加载: configured={}", key != null && !key.isBlank());
    }
}