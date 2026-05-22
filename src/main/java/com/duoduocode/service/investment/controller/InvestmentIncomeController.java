package com.duoduocode.service.investment.controller;

import com.duoduocode.service.investment.dto.InvestmentIncomeDTO;
import com.duoduocode.service.investment.dto.InvestmentIncomeVO;
import com.duoduocode.service.investment.service.InvestmentIncomeService;
import com.duoduocode.service.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 理财收益控制�? * 提供理财收益记录相关API
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
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
    public Result<Long> recordIncome(
            @PathVariable Long accountId,
            @RequestBody InvestmentIncomeDTO dto) {
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
    public Result<List<InvestmentIncomeVO>> getIncomeHistory(
            @PathVariable Long accountId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
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
    public Result<Map<String, Object>> getMonthlyIncome(
            @PathVariable Long accountId,
            @RequestParam String month) {
        Map<String, Object> result = investmentIncomeService.getMonthlyIncome(accountId, month);
        return Result.success(result);
    }

    /**
     * 获取总收�?     * GET /v1/accounts/{accountId}/income/total
     *
     * @param accountId 账户ID
     * @return 总收益统�?     */
    @GetMapping("/{accountId}/income/total")
    public Result<Map<String, Object>> getTotalIncome(@PathVariable Long accountId) {
        Map<String, Object> result = investmentIncomeService.getTotalIncome(accountId);
        return Result.success(result);
    }
}
