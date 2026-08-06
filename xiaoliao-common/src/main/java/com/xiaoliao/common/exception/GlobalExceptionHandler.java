package com.xiaoliao.common.exception;

import com.xiaoliao.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

/**
 * 全局异常处理，所有 Controller 抛出的异常在此统一转换为 Result
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.fail(400, e.getMessage());
    }

    /** @Valid 参数校验异常（M2 等模块 @RequestBody 校验触发） */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValid(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException me
                ? me.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "))
                : e.getMessage();
        log.warn("参数校验异常: {}", msg);
        return Result.fail(400, msg != null ? msg : "参数校验失败");
    }

    /** 日期格式异常 */
    @ExceptionHandler(DateTimeParseException.class)
    public Result<?> handleDateTime(DateTimeParseException e) {
        log.warn("日期格式异常: {}", e.getMessage());
        return Result.fail(400, "日期格式错误，应为 yyyy-MM-dd");
    }

    /** 兜底：未知系统异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务暂时不可用，请稍后再试");
    }
}
