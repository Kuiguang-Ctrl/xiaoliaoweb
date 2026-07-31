package com.xiaoliao.api.controller;

import com.xiaoliao.api.service.AiService;
import com.xiaoliao.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查 + 管理接口
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final AiService aiService;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean aiOnline = aiService.ping();
        return Result.ok(Map.of(
                "service", "xiaoliao-api",
                "status", "UP",
                "ai_engine", aiOnline ? "UP" : "DOWN"
        ));
    }
}
