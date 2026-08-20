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

    @Value("${xiaoliao.ai.api-token:}")
    private String apiToken;

    @Bean
    public RestClient aiRestClient() {
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(readTimeout)
        );
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(aiBaseUrl)
                .requestFactory(factory);
        // 新版 AI 引擎要求 Bearer Token 鉴权（与引擎 .env 的 API_TOKEN 一致）
        if (apiToken != null && !apiToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiToken);
        }
        return builder.build();
    }
}
