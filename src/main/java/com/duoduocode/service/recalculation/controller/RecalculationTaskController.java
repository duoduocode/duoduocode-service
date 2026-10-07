package com.duoduocode.service.recalculation.controller;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.WebExceptions;
import com.duoduocode.service.recalculation.dto.RecalculationTaskVO;
import com.duoduocode.service.recalculation.service.RecalculationTaskService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批量誊录异步任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/recalculation-tasks")
@RequiredArgsConstructor
@Tag(name = "批量誊录任务", description = "批量确认/导入异步任务的进度查询")
public class RecalculationTaskController {

    private final RecalculationTaskService recalculationTaskService;

    /**
     * 查询任务进度
     * GET /v1/recalculation-tasks/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询任务进度", description = "查询批量确认/导入异步任务的状态与计数")
    public Result<RecalculationTaskVO> getTask(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        Long userId = SecurityContext.requireUserId();
        try {
            return Result.success(recalculationTaskService.getTaskDetail(userId, id));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }
}
