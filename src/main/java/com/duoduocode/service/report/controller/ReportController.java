package com.duoduocode.service.report.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.report.dto.*;
import com.duoduocode.service.report.service.ReportService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/reports")
@Tag(name = "报表管理", description = "收支报表、账户趋势、分类分析等报表接口")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 收支报表
     * GET /v1/reports/income-expense
     */
    @GetMapping("/income-expense")
    @Operation(summary = "获取收支报表", description = "获取指定时间范围内的收支报表")
    public Result<IncomeExpenseReportVO> getIncomeExpenseReport(
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/reports/income-expense - userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        IncomeExpenseReportVO report = reportService.getIncomeExpenseReport(userId, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 账户趋势
     * GET /v1/reports/account-trend
     */
    @GetMapping("/account-trend")
    @Operation(summary = "获取账户趋势报表", description = "获取指定账户在时间范围内的余额趋势")
    public Result<AccountTrendReportVO> getAccountTrendReport(
            @Parameter(description = "账户ID") @RequestParam Long accountId,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        AccountTrendReportVO report = reportService.getAccountTrendReport(userId, accountId, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 分类分析
     * GET /v1/reports/category-analysis
     */
    @GetMapping("/category-analysis")
    @Operation(summary = "获取分类分析报表", description = "获取指定类型在时间范围内的分类支出/收入分析")
    public Result<List<CategoryAnalysisReportVO>> getCategoryAnalysisReport(
            @Parameter(description = "类型 income/expense") @RequestParam String type,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryAnalysisReportVO> report = reportService.getCategoryAnalysisReport(userId, type, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 月度对比
     * GET /v1/reports/monthly-comparison
     */
    @GetMapping("/monthly-comparison")
    @Operation(summary = "获取月度对比报表", description = "对比两个月份的收支情况")
    public Result<MonthlyComparisonVO> getMonthlyComparisonReport(
            @Parameter(description = "月份1 YYYY-MM") @RequestParam String month1,
            @Parameter(description = "月份2 YYYY-MM") @RequestParam String month2) {
        Long userId = SecurityContext.requireUserId();
        MonthlyComparisonVO report = reportService.getMonthlyComparisonReport(userId, month1, month2);
        return Result.success(report);
    }
}
