package com.duoduocode.service.notification.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.notification.service.NotificationScheduler;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知扫描测试专用入口（pytest 触发一轮扫描，不依赖等待真实 cron）。
 * 生产部署时通过 app.test.scan-endpoint.enabled=false 关闭。
 */
@Slf4j
@RestController
@RequestMapping("/v1/test/notifications")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test.scan-endpoint.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "消息中心-测试入口", description = "测试专用：手动触发通知扫描")
public class TestNotificationController {

    private final NotificationScheduler notificationScheduler;

    /**
     * 手动触发一轮扫描
     * POST /v1/test/notifications/scan
     */
    @PostMapping("/scan")
    @Operation(summary = "触发通知扫描", description = "测试专用：立即执行一轮三类事件扫描，返回各事件新增条数")
    public Result<Map<String, Integer>> scan() {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/test/notifications/scan - userId={}", userId);
        return Result.success(notificationScheduler.scanOnce());
    }
}
