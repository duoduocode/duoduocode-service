package com.duoduocode.service.account.controller;

import com.duoduocode.service.account.service.AccountService;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.transaction.dto.TransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 账户控制器
 * 提供账户管理相关API
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 获取账户列表
     * GET /v1/accounts
     *
     * @return 账户列表及汇总信息
     */
    @GetMapping
    public Result<Map<String, Object>> getAccountList() {
        Long userId = SecurityContext.requireUserId();
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
    public Result<Map<String, Object>> getAccountDetail(@PathVariable Long id) {
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
    public Result<Long> createAccount(@RequestBody Map<String, Object> dto) {
        Long userId = SecurityContext.requireUserId();
        Long accountId = accountService.createAccount(userId, dto);
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
    public Result<Void> updateAccount(@PathVariable Long id, @RequestBody Map<String, Object> dto) {
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
    public Result<Void> deleteAccount(@PathVariable Long id) {
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
    public Result<PageResult<TransactionVO>> getAccountTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
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
    public Result<Void> adjustBalance(@PathVariable Long id, @RequestBody Map<String, Object> dto) {
        BigDecimal newBalance = new BigDecimal(dto.get("newBalance").toString());
        String reason = (String) dto.get("reason");
        accountService.adjustBalance(id, newBalance, reason);
        return Result.success("调整成功", null);
    }
}
