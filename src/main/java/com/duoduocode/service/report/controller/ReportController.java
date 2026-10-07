package com.duoduocode.service.report.controller;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.WebExceptions;
import com.duoduocode.service.report.dto.*;
import com.duoduocode.service.report.service.HealthService;
import com.duoduocode.service.report.service.MonthlyReportService;
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

    @Autowired
    private MonthlyReportService monthlyReportService;

    @Autowired
    private HealthService healthService;

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

    /**
     * R004 月报列表
     * GET /v1/reports/monthly?months=12
     */
    @GetMapping("/monthly")
    @Operation(summary = "月报列表", description = "近 N 个完整月倒序（不含当月），已生成/未生成两态行")
    public Result<MonthlyReportListVO> listMonthlyReports(
            @Parameter(description = "窗口月数，默认 12，上限 12")
            @RequestParam(required = false) Integer months) {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/reports/monthly - userId={}, months={}", userId, months);
        return Result.success(monthlyReportService.list(userId, months == null ? 12 : months));
    }

    /**
     * R004 月报生成（幂等）
     * POST /v1/reports/monthly/{month}/generate
     */
    @PostMapping("/monthly/{month}/generate")
    @Operation(summary = "生成月报", description = "生成指定完整月的月报快照；已存在直接返回既有快照（不重算）")
    public Result<MonthlyReportDetailVO> generateMonthlyReport(
            @Parameter(description = "月份 YYYY-MM，须为近 12 个完整月内") @PathVariable String month) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/reports/monthly/{}/generate - userId={}", month, userId);
        try {
            return Result.success(monthlyReportService.generate(userId, month));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }

    /**
     * R004 月报详情
     * GET /v1/reports/monthly/{id}
     */
    @GetMapping("/monthly/{id}")
    @Operation(summary = "月报详情", description = "按月报ID获取详情（通知 refId 同源直达；不存在或非本人 404）")
    public Result<MonthlyReportDetailVO> getMonthlyReportDetail(
            @Parameter(description = "月报ID") @PathVariable Long id) {
        log.debug("GET /v1/reports/monthly/{} ", id);
        try {
            return Result.success(monthlyReportService.getDetail(id));
        } catch (BusinessException e) {
            throw WebExceptions.fromBusiness(e);
        }
    }

    /**
     * R004 资产负债健康度
     * GET /v1/reports/health
     */
    @GetMapping("/health")
    @Operation(summary = "资产负债健康度", description = "负债率/流动比率/应急金月数三指标（区间判定与降级原因）")
    public Result<HealthVO> getHealthMetrics() {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/reports/health - userId={}", userId);
        return Result.success(healthService.getHealth(userId));
    }
}
