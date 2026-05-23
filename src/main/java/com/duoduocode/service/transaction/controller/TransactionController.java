package com.duoduocode.service.transaction.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.transaction.dto.*;
import com.duoduocode.service.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交易控制器
 * 提供交易管理相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "交易管理", description = "交易的增删改查、退款及重复检测")
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
    @Operation(summary = "获取交易列表", description = "分页获取交易列表，支持多种筛选条件")
    public Result<PageResult<TransactionVO>> getTransactionList(@Parameter(description = "查询参数") TransactionQuery query) {
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
    @Operation(summary = "获取最近交易", description = "获取最近的交易记录")
    public Result<List<TransactionVO>> getRecentTransactions(
            @Parameter(description = "限制条数") @RequestParam(defaultValue = "10") int limit) {
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
    @Operation(summary = "获取交易详情", description = "根据交易ID获取交易详细信息")
    public Result<TransactionVO> getTransactionDetail(@Parameter(description = "交易ID") @PathVariable Long id) {
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
    @Operation(summary = "创建交易", description = "创建新的交易记录")
    public Result<Long> createTransaction(@Parameter(description = "交易数据") @RequestBody TransactionDTO dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/transactions - userId={}, type={}, amount={}", userId, dto.getTransactionType(), dto.getAmount());
        Long transactionId = transactionService.createTransaction(userId, dto);
        log.info("交易创建成功, transactionId={}", transactionId);
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
    @Operation(summary = "更新交易", description = "更新交易信息")
    public Result<Void> updateTransaction(@Parameter(description = "交易ID") @PathVariable Long id, @Parameter(description = "交易数据") @RequestBody TransactionDTO dto) {
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
    @Operation(summary = "删除交易", description = "删除指定交易")
    public Result<Void> deleteTransaction(@Parameter(description = "交易ID") @PathVariable Long id) {
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
    @Operation(summary = "退款处理", description = "对指定交易进行退款操作")
    public Result<Void> refundTransaction(@Parameter(description = "交易ID") @PathVariable Long id, @Parameter(description = "退款数据") @RequestBody RefundDTO dto) {
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
    @Operation(summary = "重复交易检测", description = "检测是否存在重复交易")
    public Result<Map<String, Object>> checkDuplicate(@Parameter(description = "检测参数") @RequestBody DuplicateCheckDTO dto) {
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
    @Operation(summary = "搜索筛选交易", description = "增强版搜索，支持关键词、金额范围、分类、账户等多条件筛选")
    public Result<PageResult<TransactionVO>> searchTransactions(@Parameter(description = "搜索参数") TransactionSearchQuery query) {
        Long userId = SecurityContext.requireUserId();
        PageResult<TransactionVO> result = transactionService.searchTransactions(userId, query);
        return Result.success(result);
    }
}
