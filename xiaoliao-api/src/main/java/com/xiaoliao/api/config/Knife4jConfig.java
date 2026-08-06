package com.xiaoliao.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 配置 — 访问 http://localhost:8080/doc.html
 * <p>
 * 登录凭证统一在 Knife4j 页面的「全局参数设置」里填一次：
 * 参数类型=header，参数名称=Authorization，参数值=Bearer <token>。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("小辽 API")
                        .version("v1")
                        .description("老年积极心理 AI 助手接口文档"));
    }
}
