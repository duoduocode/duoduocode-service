package com.duoduocode.service.report.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.report.MonthlyReportScheduler;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 月报调度测试专用入口（pytest 触发一轮生成，不依赖等待真实 cron）。
 * 生产部署时通过 app.test.scan-endpoint.enabled=false 关闭。
 */
@Slf4j
@RestController
@RequestMapping("/v1/test/monthly-reports")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test.scan-endpoint.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "月报-测试入口", description = "测试专用：手动触发月报调度")
public class TestMonthlyReportController {

    private final MonthlyReportScheduler monthlyReportScheduler;

    /**
     * 手动触发一轮上月月报生成
     * POST /v1/test/monthly-reports/generate-last-month
     */
    @PostMapping("/generate-last-month")
    @Operation(summary = "触发月报调度", description = "测试专用：立即执行一轮上月月报生成（等价于每月 1 日定时任务）")
    public Result<Void> generateLastMonth() {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/test/monthly-reports/generate-last-month - userId={}", userId);
        monthlyReportScheduler.generateLastMonthReports();
        return Result.success("已触发", null);
    }
}
