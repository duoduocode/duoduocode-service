package com.duoduocode.service.investment.controller;

import com.duoduocode.service.investment.dto.InvestmentValueDTO;
import com.duoduocode.service.investment.dto.InvestmentValueVO;
import com.duoduocode.service.investment.service.InvestmentValueService;
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
 * 投资市值控制器
 * 提供投资市值记录相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "投资市值管理", description = "投资账户的市值记录及收益计算")
public class InvestmentValueController {

    private final InvestmentValueService investmentValueService;

    /**
     * 记录市�?     * POST /v1/accounts/{accountId}/market-value
     *
     * @param accountId 账户ID
     * @param dto 市值数�?     * @return 创建的记录ID
     */
    @PostMapping("/{accountId}/market-value")
    @Operation(summary = "记录市值", description = "为指定投资账户记录当前市值")
    public Result<Long> recordMarketValue(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "市值数据") @RequestBody InvestmentValueDTO dto) {
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
    @Operation(summary = "获取市值历史", description = "获取指定投资账户在时间范围内的市值变化历史")
    public Result<List<InvestmentValueVO>> getMarketValueHistory(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam String startDate,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam String endDate) {
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
    @Operation(summary = "获取最新市值", description = "获取指定投资账户的最新市值记录")
    public Result<InvestmentValueVO> getLatestMarketValue(@Parameter(description = "账户ID") @PathVariable Long accountId) {
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
    @Operation(summary = "计算收益", description = "计算指定投资账户的收益情况")
    public Result<Map<String, Object>> calculateProfit(@Parameter(description = "账户ID") @PathVariable Long accountId) {
        Map<String, Object> profit = investmentValueService.calculateProfit(accountId);
        return Result.success(profit);
    }
}
