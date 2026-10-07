package com.duoduocode.service.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理带 HTTP 状态的异常（R003 新端点参数/权限校验）：状态码透传，body 仍为 Result 结构
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException e) {
        org.springframework.http.HttpStatus status = e.getStatus();
        return org.springframework.http.ResponseEntity.status(status)
                .body(Result.fail(status.value(), e.getReason()));
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统错误: " + e.getMessage());
    }
}
