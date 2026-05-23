package com.duoduocode.service.investment.controller;

import com.duoduocode.service.investment.dto.InvestmentIncomeDTO;
import com.duoduocode.service.investment.dto.InvestmentIncomeVO;
import com.duoduocode.service.investment.service.InvestmentIncomeService;
import com.duoduocode.service.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 理财收益控制器
 * 提供理财收益记录相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "理财收益管理", description = "投资账户的理财收益记录及统计")
public class InvestmentIncomeController {

    private final InvestmentIncomeService investmentIncomeService;

    /**
     * 记录收益
     * POST /v1/accounts/{accountId}/income
     *
     * @param accountId 账户ID
     * @param dto 收益数据
     * @return 创建的记录ID
     */
    @PostMapping("/{accountId}/income")
    @Operation(summary = "记录收益", description = "为指定投资账户记录理财收益")
    public Result<Long> recordIncome(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "收益数据") @RequestBody InvestmentIncomeDTO dto) {
        Long id = investmentIncomeService.recordIncome(accountId, dto);
        return Result.success("记录成功", id);
    }

    /**
     * 获取收益历史
     * GET /v1/accounts/{accountId}/income/history
     *
     * @param accountId 账户ID
     * @param startDate 开始日�?     * @param endDate 结束日期
     * @return 收益历史列表
     */
    @GetMapping("/{accountId}/income/history")
    @Operation(summary = "获取收益历史", description = "获取指定投资账户在时间范围内的收益历史")
    public Result<List<InvestmentIncomeVO>> getIncomeHistory(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam String endDate) {
        List<InvestmentIncomeVO> history = investmentIncomeService.getIncomeHistory(
                accountId, startDate, endDate);
        return Result.success(history);
    }

    /**
     * 获取月度收益
     * GET /v1/accounts/{accountId}/income/monthly
     *
     * @param accountId 账户ID
     * @param month 月份 YYYY-MM
     * @return 月度收益统计
     */
    @GetMapping("/{accountId}/income/monthly")
    @Operation(summary = "获取月度收益", description = "获取指定投资账户在指定月份的收益统计")
    public Result<Map<String, Object>> getMonthlyIncome(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "月份 YYYY-MM") @RequestParam String month) {
        Map<String, Object> result = investmentIncomeService.getMonthlyIncome(accountId, month);
        return Result.success(result);
    }

    /**
     * 获取总收�?     * GET /v1/accounts/{accountId}/income/total
     *
     * @param accountId 账户ID
     * @return 总收益统�?     */
    @GetMapping("/{accountId}/income/total")
    @Operation(summary = "获取总收益", description = "获取指定投资账户的总收益统计")
    public Result<Map<String, Object>> getTotalIncome(@Parameter(description = "账户ID") @PathVariable Long accountId) {
        Map<String, Object> result = investmentIncomeService.getTotalIncome(accountId);
        return Result.success(result);
    }
}
