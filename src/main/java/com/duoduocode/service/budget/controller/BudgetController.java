package com.duoduocode.service.budget.controller;

import com.duoduocode.service.budget.dto.*;
import com.duoduocode.service.budget.service.BudgetCarryoverService;
import com.duoduocode.service.budget.service.BudgetService;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预算控制器
 * 提供预算管理相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
@Tag(name = "预算管理", description = "日常预算、专项预算及预算结转管理")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetCarryoverService budgetCarryoverService;

    // ===== 日常预算相关 =====

    /**
     * 获取日常预算
     * GET /v1/budgets/daily
     *
     * @param month 月份 YYYY-MM
     * @return 日常预算列表
     */
    @GetMapping("/daily")
    @Operation(summary = "获取日常预算", description = "获取指定月份的日常预算列表")
    public Result<List<DailyBudgetVO>> getDailyBudget(@Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        List<DailyBudgetVO> result = budgetService.getDailyBudget(userId, month);
        return Result.success(result);
    }

    /**
     * 设置日常预算
     * PUT /v1/budgets/daily
     *
     * @param dto 日常预算数据
     * @return 操作结果
     */
    @PutMapping("/daily")
    @Operation(summary = "设置日常预算", description = "设置或更新日常预算")
    public Result<Void> setDailyBudget(@Parameter(description = "日常预算数据") @RequestBody DailyBudgetDTO dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("PUT /v1/budgets/daily - userId={}, categoryId={}", userId, dto.getCategoryId());
        budgetService.setDailyBudget(userId, dto);
        return Result.success("设置成功", null);
    }

    /**
     * 获取预算使用情况
     * GET /v1/budgets/daily/usage
     *
     * @param month 月份 YYYY-MM
     * @return 预算使用情况列表
     */
    @GetMapping("/daily/usage")
    @Operation(summary = "获取预算使用情况", description = "获取指定月份的预算使用情况")
    public Result<List<DailyBudgetVO>> getDailyBudgetUsage(@Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        List<DailyBudgetVO> result = budgetService.getDailyBudgetUsage(userId, month);
        return Result.success(result);
    }

    // ===== 预算结转相关 =====

    /**
     * 执行预算结转
     * POST /v1/budgets/daily/carryover
     *
     * @param dto 结转数据
     * @return 创建的结转记录ID
     */
    @PostMapping("/daily/carryover")
    @Operation(summary = "执行预算结转", description = "将未使用的预算结转至下一期")
    public Result<Long> carryoverBudget(@Parameter(description = "结转数据") @RequestBody CarryoverDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Long id = budgetCarryoverService.carryoverBudget(userId, dto);
        return Result.success("结转成功", id);
    }

    /**
     * 获取结转历史
     * GET /v1/budgets/daily/carryover/history
     *
     * @param month 月份 YYYY-MM
     * @return 结转历史列表
     */
    @GetMapping("/daily/carryover/history")
    @Operation(summary = "获取结转历史", description = "获取指定月份的预算结转历史记录")
    public Result<List<Map<String, Object>>> getCarryoverHistory(@Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        List<Map<String, Object>> result = budgetCarryoverService.getCarryoverHistory(userId, month);
        return Result.success(result);
    }

    /**
     * 计算可结转金额
     * GET /v1/budgets/daily/carryover/calculate
     *
     * @param categoryId 分类ID
     * @param fromMonth 来源月份 YYYY-MM
     * @return 可结转金额
     */
    @GetMapping("/daily/carryover/calculate")
    @Operation(summary = "计算可结转金额", description = "计算指定分类在指定月份的可结转预算金额")
    public Result<BigDecimal> calculateCarryoverAmount(
            @Parameter(description = "分类ID") @RequestParam Long categoryId,
            @Parameter(description = "来源月份 YYYY-MM") @RequestParam String fromMonth) {
        Long userId = SecurityContext.requireUserId();
        BigDecimal amount = budgetCarryoverService.calculateCarryoverAmount(userId, categoryId, fromMonth);
        return Result.success(amount);
    }

    /**
     * 获取结转统计信息
     * GET /v1/budgets/daily/carryover/statistics
     *
     * @param month 月份 YYYY-MM
     * @return 结转统计
     */
    @GetMapping("/daily/carryover/statistics")
    @Operation(summary = "获取结转统计", description = "获取指定月份的预算结转统计信息")
    public Result<Map<String, Object>> getCarryoverStatistics(@Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        Map<String, Object> result = budgetCarryoverService.getCarryoverStatistics(userId, month);
        return Result.success(result);
    }

    // ===== 专项预算相关 =====

    /**
     * 获取专项预算列表
     * GET /v1/budgets/special
     *
     * @return 专项预算列表
     */
    @GetMapping("/special")
    @Operation(summary = "获取专项预算列表", description = "获取当前用户的所有专项预算")
    public Result<List<SpecialBudgetVO>> getSpecialBudgetList() {
        Long userId = SecurityContext.requireUserId();
        List<SpecialBudgetVO> result = budgetService.getSpecialBudgetList(userId);
        return Result.success(result);
    }

    /**
     * 获取专项预算详情
     * GET /v1/budgets/special/{id}
     *
     * @param id 预算ID
     * @return 专项预算详情
     */
    @GetMapping("/special/{id}")
    @Operation(summary = "获取专项预算详情", description = "根据ID获取专项预算详情")
    public Result<SpecialBudgetVO> getSpecialBudgetDetail(@Parameter(description = "预算ID") @PathVariable Long id) {
        SpecialBudgetVO result = budgetService.getSpecialBudgetDetail(id);
        return Result.success(result);
    }

    /**
     * 创建专项预算
     * POST /v1/budgets/special
     *
     * @param dto 专项预算数据
     * @return 创建的预算ID
     */
    @PostMapping("/special")
    @Operation(summary = "创建专项预算", description = "创建新的专项预算")
    public Result<Long> createSpecialBudget(@Parameter(description = "专项预算数据") @RequestBody SpecialBudgetDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Long id = budgetService.createSpecialBudget(userId, dto);
        return Result.success("创建成功", id);
    }

    /**
     * 更新专项预算
     * PUT /v1/budgets/special/{id}
     *
     * @param id 预算ID
     * @param dto 专项预算数据
     * @return 操作结果
     */
    @PutMapping("/special/{id}")
    @Operation(summary = "更新专项预算", description = "更新专项预算信息")
    public Result<Void> updateSpecialBudget(@Parameter(description = "预算ID") @PathVariable Long id, @Parameter(description = "专项预算数据") @RequestBody SpecialBudgetDTO dto) {
        budgetService.updateSpecialBudget(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 结束专项预算
     * POST /v1/budgets/special/{id}/complete
     *
     * @param id 预算ID
     * @return 操作结果
     */
    @PostMapping("/special/{id}/complete")
    @Operation(summary = "结束专项预算", description = "结束指定的专项预算")
    public Result<Void> completeSpecialBudget(@Parameter(description = "预算ID") @PathVariable Long id) {
        budgetService.completeSpecialBudget(id);
        return Result.success("预算已结束", null);
    }

    /**
     * 获取可用专项预算
     * GET /v1/budgets/special/available
     *
     * @return 可用专项预算列表
     */
    @GetMapping("/special/available")
    @Operation(summary = "获取可用专项预算", description = "获取当前用户可用的专项预算列表")
    public Result<List<SpecialBudgetVO>> getAvailableSpecialBudgets() {
        Long userId = SecurityContext.requireUserId();
        List<SpecialBudgetVO> result = budgetService.getAvailableSpecialBudgets(userId);
        return Result.success(result);
    }
}
