package com.duoduocode.service.investment.controller;

import com.duoduocode.service.investment.dto.InvestmentValueDTO;
import com.duoduocode.service.investment.dto.InvestmentValueVO;
import com.duoduocode.service.investment.service.InvestmentValueService;
import com.duoduocode.service.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 投资市值控制器
 * 提供投资市值记录相关API
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class InvestmentValueController {

    private final InvestmentValueService investmentValueService;

    /**
     * 记录市�?     * POST /v1/accounts/{accountId}/market-value
     *
     * @param accountId 账户ID
     * @param dto 市值数�?     * @return 创建的记录ID
     */
    @PostMapping("/{accountId}/market-value")
    public Result<Long> recordMarketValue(
            @PathVariable Long accountId,
            @RequestBody InvestmentValueDTO dto) {
        Long id = investmentValueService.recordMarketValue(accountId, dto);
        return Result.success("记录成功", id);
    }

    /**
     * 获取市值历�?     * GET /v1/accounts/{accountId}/market-value/history
     *
     * @param accountId 账户ID
     * @param startDate 开始日�?     * @param endDate 结束日期
     * @return 市值历史列�?     */
    @GetMapping("/{accountId}/market-value/history")
    public Result<List<InvestmentValueVO>> getMarketValueHistory(
            @PathVariable Long accountId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<InvestmentValueVO> history = investmentValueService.getMarketValueHistory(
                accountId, startDate, endDate);
        return Result.success(history);
    }

    /**
     * 获取最新市�?     * GET /v1/accounts/{accountId}/market-value/latest
     *
     * @param accountId 账户ID
     * @return 最新市值记�?     */
    @GetMapping("/{accountId}/market-value/latest")
    public Result<InvestmentValueVO> getLatestMarketValue(@PathVariable Long accountId) {
        InvestmentValueVO value = investmentValueService.getLatestMarketValue(accountId);
        return Result.success(value);
    }

    /**
     * 计算收益
     * GET /v1/accounts/{accountId}/market-value/profit
     *
     * @param accountId 账户ID
     * @return 收益信息
     */
    @GetMapping("/{accountId}/market-value/profit")
    public Result<Map<String, Object>> calculateProfit(@PathVariable Long accountId) {
        Map<String, Object> profit = investmentValueService.calculateProfit(accountId);
        return Result.success(profit);
    }
}
