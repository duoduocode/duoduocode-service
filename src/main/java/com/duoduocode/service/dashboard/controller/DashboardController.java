package com.duoduocode.service.dashboard.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.dashboard.dto.DashboardVO;
import com.duoduocode.service.dashboard.service.DashboardService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Dashboard 控制器
 * 提供首页数据看板相关接口
 */
@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "首页看板", description = "首页数据汇总、净资产趋势、月度收支等看板接口")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取首页数据看板
     * GET /v1/dashboard
     *
     * @return 首页汇总数据
     */
    @GetMapping
    @Operation(summary = "获取首页看板", description = "获取当前用户的首页数据看板汇总")
    public Result<DashboardVO> getDashboard() {
        Long userId = SecurityContext.requireUserId();
        DashboardVO vo = dashboardService.getDashboard(userId);
        return Result.success(vo);
    }

    /**
     * 获取首页数据看板（别名）
     * GET /v1/dashboard/summary
     *
     * @return 首页汇总数据
     */
    @GetMapping("/summary")
    @Operation(summary = "获取首页看板摘要", description = "获取首页看板汇总数据（getDashboard的别名）")
    public Result<DashboardVO> getDashboardSummary() {
        return getDashboard();
    }

    /**
     * 获取净资产趋势
     * GET /v1/dashboard/net-worth-trend
     *
     * @param startDate 开始日期 (YYYY-MM-DD)
     * @param endDate   结束日期 (YYYY-MM-DD)
     * @return 净资产趋势数据
     */
    @GetMapping("/net-worth-trend")
    @Operation(summary = "获取净资产趋势", description = "获取指定时间范围内的净资产变化趋势")
    public Result<Map<String, Object>> getNetWorthTrend(
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = SecurityContext.requireUserId();
        Map<String, Object> result = dashboardService.getNetWorthTrend(
                userId,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        return Result.success(result);
    }

    /**
     * 获取月度收支详情
     * GET /v1/dashboard/monthly
     *
     * @param month 月份 (YYYY-MM)，默认当前月
     * @return 月度收支数据
     */
    @GetMapping("/monthly")
    @Operation(summary = "获取月度收支详情", description = "获取指定月份的收支详情数据，默认当前月")
    public Result<DashboardVO> getMonthlyDetail(
            @Parameter(description = "月份 YYYY-MM，默认当前月") @RequestParam(required = false) String month) {
        Long userId = SecurityContext.requireUserId();

        if (month == null || month.isEmpty()) {
            month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        DashboardVO vo = dashboardService.getMonthlyDetail(userId, month);
        return Result.success(vo);
    }

    /**
     * 获取今日数据
     * GET /v1/dashboard/today
     *
     * @return 今日收支数据
     */
    @GetMapping("/today")
    @Operation(summary = "获取今日数据", description = "获取今日的收支数据汇总")
    public Result<Map<String, Object>> getTodayData() {
        Long userId = SecurityContext.requireUserId();
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        DashboardVO vo = dashboardService.getDashboard(userId);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("date", todayStr);
        result.put("expense", vo.getTodayExpense());
        result.put("transactionCount", 0);

        return Result.success(result);
    }
}
