package com.duoduocode.service.budget.controller;

import com.duoduocode.service.budget.dto.*;
import com.duoduocode.service.budget.service.BudgetCarryoverService;
import com.duoduocode.service.budget.service.BudgetService;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预算控制�? * 提供预算管理相关API
 */
@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
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
    public Result<List<DailyBudgetVO>> getDailyBudget(@RequestParam String month) {
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
    public Result<Void> setDailyBudget(@RequestBody DailyBudgetDTO dto) {
        Long userId = SecurityContext.requireUserId();
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
    public Result<List<DailyBudgetVO>> getDailyBudgetUsage(@RequestParam String month) {
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
    public Result<Long> carryoverBudget(@RequestBody CarryoverDTO dto) {
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
    public Result<List<Map<String, Object>>> getCarryoverHistory(@RequestParam String month) {
        Long userId = SecurityContext.requireUserId();
        List<Map<String, Object>> result = budgetCarryoverService.getCarryoverHistory(userId, month);
        return Result.success(result);
    }

    /**
     * 计算可结转金�?     * GET /v1/budgets/daily/carryover/calculate
     *
     * @param categoryId 分类ID
     * @param fromMonth 来源月份 YYYY-MM
     * @return 可结转金�?     */
    @GetMapping("/daily/carryover/calculate")
    public Result<BigDecimal> calculateCarryoverAmount(
            @RequestParam Long categoryId,
            @RequestParam String fromMonth) {
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
    public Result<Map<String, Object>> getCarryoverStatistics(@RequestParam String month) {
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
    public Result<SpecialBudgetVO> getSpecialBudgetDetail(@PathVariable Long id) {
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
    public Result<Long> createSpecialBudget(@RequestBody SpecialBudgetDTO dto) {
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
    public Result<Void> updateSpecialBudget(@PathVariable Long id, @RequestBody SpecialBudgetDTO dto) {
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
    public Result<Void> completeSpecialBudget(@PathVariable Long id) {
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
    public Result<List<SpecialBudgetVO>> getAvailableSpecialBudgets() {
        Long userId = SecurityContext.requireUserId();
        List<SpecialBudgetVO> result = budgetService.getAvailableSpecialBudgets(userId);
        return Result.success(result);
    }
}
