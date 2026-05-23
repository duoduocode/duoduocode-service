package com.duoduocode.service.account.controller;

import com.duoduocode.service.account.service.AccountService;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.transaction.dto.TransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 账户控制器
 * 提供账户管理相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "账户管理", description = "账户的增删改查及余额调整")
public class AccountController {

    private final AccountService accountService;

    /**
     * 获取账户列表
     * GET /v1/accounts
     *
     * @return 账户列表及汇总信息
     */
    @GetMapping
    @Operation(summary = "获取账户列表", description = "获取当前用户的所有账户列表及资产汇总信息")
    public Result<Map<String, Object>> getAccountList() {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/accounts - userId={}", userId);
        Map<String, Object> result = accountService.getAccountList(userId);
        return Result.success(result);
    }

    /**
     * 获取账户详情
     * GET /v1/accounts/{id}
     *
     * @param id 账户ID
     * @return 账户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取账户详情", description = "根据账户ID获取账户详细信息")
    public Result<Map<String, Object>> getAccountDetail(@Parameter(description = "账户ID") @PathVariable Long id) {
        Map<String, Object> account = accountService.getAccountDetail(id);
        return Result.success(account);
    }

    /**
     * 创建账户
     * POST /v1/accounts
     *
     * @param dto 账户数据
     * @return 创建的账户ID
     */
    @PostMapping
    @Operation(summary = "创建账户", description = "创建新账户，支持资产、负债、投资等类型")
    public Result<Long> createAccount(@Parameter(description = "账户数据") @RequestBody Map<String, Object> dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/accounts - userId={}, name={}, type={}", userId, dto.get("name"), dto.get("type"));
        Long accountId = accountService.createAccount(userId, dto);
        log.info("账户创建成功, accountId={}", accountId);
        return Result.success("创建成功", accountId);
    }

    /**
     * 更新账户
     * PUT /v1/accounts/{id}
     *
     * @param id  账户ID
     * @param dto 账户数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新账户", description = "更新账户信息")
    public Result<Void> updateAccount(@Parameter(description = "账户ID") @PathVariable Long id, @Parameter(description = "账户数据") @RequestBody Map<String, Object> dto) {
        accountService.updateAccount(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除账户
     * DELETE /v1/accounts/{id}
     *
     * @param id 账户ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除账户", description = "删除指定账户")
    public Result<Void> deleteAccount(@Parameter(description = "账户ID") @PathVariable Long id) {
        log.info("DELETE /v1/accounts/{}", id);
        accountService.deleteAccount(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取账户交易流水
     * GET /v1/accounts/{id}/transactions
     *
     * @param id       账户ID
     * @param page     页码
     * @param pageSize 每页数量
     * @return 交易流水列表
     */
    @GetMapping("/{id}/transactions")
    @Operation(summary = "获取账户交易流水", description = "分页获取指定账户的交易记录")
    public Result<PageResult<TransactionVO>> getAccountTransactions(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<TransactionVO> result = accountService.getAccountTransactions(id, page, pageSize);
        return Result.success(result);
    }

    /**
     * 调整账户余额
     * POST /v1/accounts/{id}/adjust-balance
     *
     * @param id  账户ID
     * @param dto 调整数据
     * @return 操作结果
     */
    @PostMapping("/{id}/adjust-balance")
    @Operation(summary = "调整账户余额", description = "调整账户初始余额，用于账户初始化或纠错")
    public Result<Void> adjustBalance(@Parameter(description = "账户ID") @PathVariable Long id, @Parameter(description = "调整数据") @RequestBody Map<String, Object> dto) {
        BigDecimal newBalance = new BigDecimal(dto.get("newBalance").toString());
        String reason = (String) dto.get("reason");
        accountService.adjustBalance(id, newBalance, reason);
        return Result.success("调整成功", null);
    }
}
