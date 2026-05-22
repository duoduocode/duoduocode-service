package com.duoduocode.service.transaction.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.transaction.dto.*;
import com.duoduocode.service.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交易控制器
 * 提供交易管理相关API
 */
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * 获取交易列表
     * GET /v1/transactions
     *
     * @param query 查询参数
     * @return 分页交易列表
     */
    @GetMapping
    public Result<PageResult<TransactionVO>> getTransactionList(TransactionQuery query) {
        Long userId = SecurityContext.requireUserId();
        PageResult<TransactionVO> result = transactionService.getTransactionList(userId, query);
        return Result.success(result);
    }

    /**
     * 获取最近交易
     * GET /v1/transactions/recent
     *
     * @param limit 限制条数，默认10条
     * @return 最近交易列表
     */
    @GetMapping("/recent")
    public Result<List<TransactionVO>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityContext.requireUserId();
        List<TransactionVO> result = transactionService.getRecentTransactions(userId, limit);
        return Result.success(result);
    }

    /**
     * 获取交易详情
     * GET /v1/transactions/{id}
     *
     * @param id 交易ID
     * @return 交易详情
     */
    @GetMapping("/{id}")
    public Result<TransactionVO> getTransactionDetail(@PathVariable Long id) {
        TransactionVO transaction = transactionService.getTransactionDetail(id);
        return Result.success(transaction);
    }

    /**
     * 创建交易
     * POST /v1/transactions
     *
     * @param dto 交易数据
     * @return 创建的交易ID
     */
    @PostMapping
    public Result<Long> createTransaction(@RequestBody TransactionDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Long transactionId = transactionService.createTransaction(userId, dto);
        return Result.success("创建成功", transactionId);
    }

    /**
     * 更新交易
     * PUT /v1/transactions/{id}
     *
     * @param id  交易ID
     * @param dto 交易数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> updateTransaction(@PathVariable Long id, @RequestBody TransactionDTO dto) {
        transactionService.updateTransaction(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除交易
     * DELETE /v1/transactions/{id}
     *
     * @param id 交易ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return Result.success("删除成功", null);
    }

    /**
     * 退款处理
     * POST /v1/transactions/{id}/refund
     *
     * @param id  交易ID
     * @param dto 退款数据
     * @return 操作结果
     */
    @PostMapping("/{id}/refund")
    public Result<Void> refundTransaction(@PathVariable Long id, @RequestBody RefundDTO dto) {
        transactionService.refundTransaction(id, dto);
        return Result.success("退款成功", null);
    }

    /**
     * 重复交易检测
     * POST /v1/transactions/check-duplicate
     *
     * @param dto 检测参数
     * @return 检测结果
     */
    @PostMapping("/check-duplicate")
    public Result<Map<String, Object>> checkDuplicate(@RequestBody DuplicateCheckDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Map<String, Object> result = transactionService.checkDuplicate(userId, dto);
        return Result.success(result);
    }

    /**
     * 搜索筛选交易（增强版）
     * GET /v1/transactions/search
     *
     * @param query 搜索参数（支持 keyword, minAmount, maxAmount, categoryId, accountId, startDate, endDate, type, sortBy, sortOrder 等）
     * @return 分页交易列表
     */
    @GetMapping("/search")
    public Result<PageResult<TransactionVO>> searchTransactions(TransactionSearchQuery query) {
        Long userId = SecurityContext.requireUserId();
        PageResult<TransactionVO> result = transactionService.searchTransactions(userId, query);
        return Result.success(result);
    }
}
