package com.duoduocode.service.recurring.controller;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.WebExceptions;
import com.duoduocode.service.recurring.dto.BatchConfirmVO;
import com.duoduocode.service.recurring.dto.BatchOperateDTO;
import com.duoduocode.service.recurring.dto.RecurringCalendarVO;
import com.duoduocode.service.recurring.dto.RecurringTemplateDTO;
import com.duoduocode.service.recurring.dto.RecurringTemplateVO;
import com.duoduocode.service.recurring.dto.SubscriptionOverviewVO;
import com.duoduocode.service.recurring.service.RecurringTemplateService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 周期交易模板控制器
 * 提供周期交易模板管理相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/recurring-templates")
@RequiredArgsConstructor
@Tag(name = "周期交易模板", description = "周期交易模板的增删改查及触发管理")
public class RecurringTemplateController {

    private final RecurringTemplateService recurringTemplateService;

    /**
     * 获取周期模板列表
     * GET /v1/recurring-templates
     *
     * @return 模板列表
     */
    @GetMapping
    @Operation(summary = "获取周期模板列表", description = "获取当前用户的所有周期交易模板")
    public Result<List<RecurringTemplateVO>> getTemplateList() {
        Long userId = SecurityContext.requireUserId();
        List<RecurringTemplateVO> templates = recurringTemplateService.getTemplateList(userId);
        return Result.success(templates);
    }

    /**
     * 获取模板详情
     * GET /v1/recurring-templates/{id}
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情", description = "根据ID获取周期交易模板详情")
    public Result<RecurringTemplateVO> getTemplateDetail(@Parameter(description = "模板ID") @PathVariable Long id) {
        RecurringTemplateVO template = recurringTemplateService.getTemplateDetail(id);
        return Result.success(template);
    }

    /**
     * 创建模板
     * POST /v1/recurring-templates
     *
     * @param dto 模板数据
     * @return 创建的模板ID
     */
    @PostMapping
    @Operation(summary = "创建模板", description = "创建新的周期交易模板")
    public Result<Long> createTemplate(@Parameter(description = "模板数据") @RequestBody RecurringTemplateDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Long templateId = recurringTemplateService.createTemplate(userId, dto);
        return Result.success("创建成功", templateId);
    }

    /**
     * 更新模板
     * PUT /v1/recurring-templates/{id}
     *
     * @param id  模板ID
     * @param dto 模板数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新模板", description = "更新周期交易模板信息")
    public Result<Void> updateTemplate(@Parameter(description = "模板ID") @PathVariable Long id, @Parameter(description = "模板数据") @RequestBody RecurringTemplateDTO dto) {
        recurringTemplateService.updateTemplate(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除模板
     * DELETE /v1/recurring-templates/{id}
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板", description = "删除指定的周期交易模板")
    public Result<Void> deleteTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        recurringTemplateService.deleteTemplate(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取到期提醒列表
     * GET /v1/recurring-templates/due
     *
     * @return 到期模板列表
     */
    @GetMapping("/due")
    @Operation(summary = "获取到期提醒列表", description = "获取当前用户所有到期的周期交易模板")
    public Result<List<RecurringTemplateVO>> getDueTemplates() {
        Long userId = SecurityContext.requireUserId();
        List<RecurringTemplateVO> templates = recurringTemplateService.getDueTemplates(userId);
        return Result.success(templates);
    }

    /**
     * 手动触发提醒
     * POST /v1/recurring-templates/{id}/trigger
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/trigger")
    @Operation(summary = "手动触发提醒", description = "手动触发指定周期交易模板的提醒")
    public Result<Void> triggerTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        recurringTemplateService.triggerTemplate(id);
        return Result.success("触发成功", null);
    }

    /**
     * 暂停模板
     * POST /v1/recurring-templates/{id}/pause
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停模板", description = "暂停指定的周期交易模板")
    public Result<Void> pauseTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        recurringTemplateService.pauseTemplate(id);
        return Result.success("暂停成功", null);
    }

    /**
     * 恢复模板
     * POST /v1/recurring-templates/{id}/resume
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复模板", description = "恢复已暂停的周期交易模板")
    public Result<Void> resumeTemplate(@Parameter(description = "模板ID") @PathVariable Long id) {
        recurringTemplateService.resumeTemplate(id);
        return Result.success("恢复成功", null);
    }

    /**
     * R003 批量确认：勾选 ≥10 笔转异步任务
     * POST /v1/recurring-templates/batch-confirm
     */
    @PostMapping("/batch-confirm")
    @Operation(summary = "批量确认模板", description = "逐笔确认落账；勾选≥10 笔转异步任务并返回 taskId，单笔失败不影响其他笔")
    public Result<BatchConfirmVO> batchConfirm(
            @Parameter(description = "模板ID列表") @RequestBody BatchOperateDTO dto) {
        Long userId = SecurityContext.requireUserId();
        try {
            return Result.success(recurringTemplateService.batchConfirm(
                    userId, dto == null ? null : dto.getTemplateIds()));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }

    /**
     * R003 批量跳过：推进模板+消化待办，不落账
     * POST /v1/recurring-templates/batch-skip
     */
    @PostMapping("/batch-skip")
    @Operation(summary = "批量跳过模板", description = "逐笔跳过当期（推进模板+消化待办，不产生交易），恒同步执行")
    public Result<BatchConfirmVO> batchSkip(
            @Parameter(description = "模板ID列表") @RequestBody BatchOperateDTO dto) {
        Long userId = SecurityContext.requireUserId();
        try {
            return Result.success(recurringTemplateService.batchSkip(
                    userId, dto == null ? null : dto.getTemplateIds()));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }

    /**
     * R003 月度日历：模板计划期平铺
     * GET /v1/recurring-templates/calendar?month=YYYY-MM
     */
    @GetMapping("/calendar")
    @Operation(summary = "周期模板月度日历", description = "按月返回全部 active 模板的计划期与三态状态（done/due/planned）")
    public Result<RecurringCalendarVO> getCalendar(
            @Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        try {
            return Result.success(recurringTemplateService.getCalendar(userId, month));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }

    /**
     * R004 订阅管理总览
     * GET /v1/recurring-templates/subscription/overview
     */
    @GetMapping("/subscription/overview")
    @Operation(summary = "订阅管理总览", description = "active 支出模板折算月额合计、逐项涨价三态与近 30 天计划扣款窗口")
    public Result<SubscriptionOverviewVO> getSubscriptionOverview() {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/recurring-templates/subscription/overview - userId={}", userId);
        return Result.success(recurringTemplateService.getSubscriptionOverview(userId));
    }
}
