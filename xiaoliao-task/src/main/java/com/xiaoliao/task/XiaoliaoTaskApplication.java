package com.xiaoliao.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务模块入口
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.xiaoliao.task")
public class XiaoliaoTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaoliaoTaskApplication.class, args);
    }
}
