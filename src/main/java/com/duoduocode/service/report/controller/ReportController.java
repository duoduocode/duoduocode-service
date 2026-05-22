package com.duoduocode.service.report.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.report.dto.*;
import com.duoduocode.service.report.service.ReportService;
import com.duoduocode.service.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 收支报表
     * GET /v1/reports/income-expense
     */
    @GetMapping("/income-expense")
    public Result<IncomeExpenseReportVO> getIncomeExpenseReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        IncomeExpenseReportVO report = reportService.getIncomeExpenseReport(userId, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 账户趋势
     * GET /v1/reports/account-trend
     */
    @GetMapping("/account-trend")
    public Result<AccountTrendReportVO> getAccountTrendReport(
            @RequestParam Long accountId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        AccountTrendReportVO report = reportService.getAccountTrendReport(userId, accountId, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 分类分析
     * GET /v1/reports/category-analysis
     */
    @GetMapping("/category-analysis")
    public Result<List<CategoryAnalysisReportVO>> getCategoryAnalysisReport(
            @RequestParam String type,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryAnalysisReportVO> report = reportService.getCategoryAnalysisReport(userId, type, startDate, endDate);
        return Result.success(report);
    }

    /**
     * 月度对比
     * GET /v1/reports/monthly-comparison
     */
    @GetMapping("/monthly-comparison")
    public Result<MonthlyComparisonVO> getMonthlyComparisonReport(
            @RequestParam String month1,
            @RequestParam String month2) {
        Long userId = SecurityContext.requireUserId();
        MonthlyComparisonVO report = reportService.getMonthlyComparisonReport(userId, month1, month2);
        return Result.success(report);
    }
}
