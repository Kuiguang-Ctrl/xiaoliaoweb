package com.xiaoliao.api.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口指标注解 —— 标注后由 {@link ApiMetricAspect} 统一采集调用量、耗时、失败数。
 * <p>
 * 指标通过 Micrometer 输出到 {@code /actuator/prometheus}，可接入 Prometheus + Grafana
 * 搭建接口监控大盘；异常抛出前仍会记录失败计数，不影响业务主流程。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMetric {

    /**
     * 业务指标名，如 "chat.ai"、"checkin.check"；为空时使用「类名.方法名」
     */
    String value() default "";
}
