package com.xiaoliao.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 小辽 — 主服务入口
 */
@SpringBootApplication(scanBasePackages = "com.xiaoliao")
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class XiaoliaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoliaoApplication.class, args);
    }
}
