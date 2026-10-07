package com.duoduocode.service.recalculation.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.recalculation.service.RecalculationTaskService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 超时任务扫描测试专用入口（pytest 触发一轮扫描，不依赖等待真实定时）。
 * 生产部署时通过 app.test.scan-endpoint.enabled=false 关闭。
 */
@Slf4j
@RestController
@RequestMapping("/v1/test/recalc-tasks")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test.scan-endpoint.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "批量誊录任务-测试入口", description = "测试专用：手动触发超时任务扫描")
public class TestRecalcTaskController {

    private final RecalculationTaskService recalculationTaskService;

    /**
     * 手动触发一轮超时扫描
     * POST /v1/test/recalc-tasks/sweep
     */
    @PostMapping("/sweep")
    @Operation(summary = "触发超时任务扫描", description = "测试专用：将超 30 分钟未完成的 running/pending 任务置 failed")
    public Result<Integer> sweep() {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/test/recalc-tasks/sweep - userId={}", userId);
        return Result.success(recalculationTaskService.failStaleTasks(LocalDateTime.now()));
    }
}
