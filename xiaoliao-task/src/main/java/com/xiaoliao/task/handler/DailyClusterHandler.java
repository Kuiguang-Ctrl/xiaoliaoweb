package com.xiaoliao.task.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日错误聚类定时任务
 *
 * 流程：
 *   1. 读取昨日 score < 3 或有 error_tags 的对话
 *   2. 按 error_tag 分组聚类
 *   3. 超过阈值的类调用 Python AI 生成 Prompt 补丁
 *   4. 向量化后写入 lessons 表
 */
@Slf4j
@Component
public class DailyClusterHandler {

    /**
     * 每天凌晨 3:00 执行错误聚类
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void execute() {
        log.info("=== 每日错误聚类任务开始 ===");

        // TODO Phase 1 完成后实现：
        // 1. 查询昨日低分对话
        // 2. 按 error_tag 分组
        // 3. 调 Python /training/cluster 接口
        // 4. 结果写入 lessons 表

        log.info("=== 每日错误聚类任务完成 ===");
    }
}
