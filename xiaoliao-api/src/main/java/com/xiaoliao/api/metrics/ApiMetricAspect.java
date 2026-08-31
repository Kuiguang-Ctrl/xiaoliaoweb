package com.xiaoliao.api.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ApiMetric} 切面 —— 统一采集被标注方法的调用量/耗时/失败数。
 * <p>
 * 指标：xiaoliao_api_seconds（耗时 Timer）、xiaoliao_api_total（调用次数 Counter，按 result 打标）。
 * 指标对象按 api 名称缓存，避免每次调用重复注册。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ApiMetricAspect {

    private static final String METRIC_TIMER = "xiaoliao_api_seconds";
    private static final String METRIC_COUNTER = "xiaoliao_api_total";
    private static final String TAG_API = "api";
    private static final String TAG_RESULT = "result";

    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    @Around("@annotation(apiMetric)")
    public Object around(ProceedingJoinPoint joinPoint, ApiMetric apiMetric) throws Throwable {
        String api = apiMetric.value().isBlank()
                ? joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName()
                : apiMetric.value();

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Object result = joinPoint.proceed();
            sample.stop(timer(api));
            counter(api, "success").increment();
            return result;
        } catch (Throwable t) {
            // 先记录失败指标再抛异常，保证异常语义不被切面吞掉
            sample.stop(timer(api));
            counter(api, "failure").increment();
            throw t;
        }
    }

    private Timer timer(String api) {
        return timerCache.computeIfAbsent(api, key -> Timer.builder(METRIC_TIMER)
                .description("接口调用耗时（含成功与失败）")
                .tag(TAG_API, key)
                .register(meterRegistry));
    }

    private Counter counter(String api, String result) {
        String cacheKey = api + "|" + result;
        return counterCache.computeIfAbsent(cacheKey, key -> Counter.builder(METRIC_COUNTER)
                .description("接口调用次数（按结果）")
                .tag(TAG_API, api)
                .tag(TAG_RESULT, result)
                .register(meterRegistry));
    }
}
