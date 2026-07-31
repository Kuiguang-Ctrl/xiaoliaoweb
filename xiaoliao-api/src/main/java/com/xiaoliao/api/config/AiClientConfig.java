package com.xiaoliao.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Python AI 引擎 HTTP 客户端配置
 */
@Configuration
public class AiClientConfig {

    @Value("${xiaoliao.ai.base-url:http://localhost:8081}")
    private String aiBaseUrl;

    @Value("${xiaoliao.ai.connect-timeout:3s}")
    private Duration connectTimeout;

    @Value("${xiaoliao.ai.read-timeout:15s}")
    private Duration readTimeout;

    @Bean
    public RestClient aiRestClient() {
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(readTimeout)
        );
        return RestClient.builder()
                .baseUrl(aiBaseUrl)
                .requestFactory(factory)
                .build();
    }
}
