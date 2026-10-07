package com.duoduocode.service.billimport;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收/付款方式 → 账户匹配器。
 * 同名账户命中；未知方式归「现金」；未命中按原文命名创建（asset，初始余额 0，批内去重）。
 */
@Component
@RequiredArgsConstructor
public class BillAccountMatcher {

    public static final String CASH_ACCOUNT_NAME = "现金";

    private final AccountMapper accountMapper;

    /**
     * 匹配或创建账户
     *
     * @param userId    用户ID
     * @param payMethod 收/付款方式原文
     * @param created   批内已创建账户（跨行去重；新创建的账户追加到此列表）
     */
    public MatchResult match(Long userId, String payMethod, List<Account> created) {
        String name = payMethod == null ? "" : payMethod.trim();

        List<Account> existing = accountMapper.selectByUserId(userId);
        if (name.isEmpty()) {
            Account cash = findByName(existing, CASH_ACCOUNT_NAME);
            if (cash != null) {
                return new MatchResult(cash.getId(), false);
            }
            return createAccount(userId, CASH_ACCOUNT_NAME, created);
        }

        Account hit = findByName(existing, name);
        if (hit != null) {
            return new MatchResult(hit.getId(), false);
        }

        Account batchHit = findByName(created, name);
        if (batchHit != null) {
            return new MatchResult(batchHit.getId(), true);
        }
        return createAccount(userId, name, created);
    }

    private MatchResult createAccount(Long userId, String name, List<Account> created) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName(name);
        account.setType("asset");
        // 与前端建账户默认一致：asset 印章「资」/ 靛色
        account.setIcon("资");
        account.setColor("#33586B");
        account.setInitialBalance(BigDecimal.ZERO);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(BigDecimal.ZERO);
        account.setSortOrder(0);
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        created.add(account);
        return new MatchResult(account.getId(), true);
    }

    private Account findByName(List<Account> accounts, String name) {
        if (accounts == null) {
            return null;
        }
        for (Account account : accounts) {
            if (name.equals(account.getName())) {
                return account;
            }
        }
        return null;
    }

    /** 匹配结果 */
    public static class MatchResult {

        private final Long accountId;
        private final boolean created;

        public MatchResult(Long accountId, boolean created) {
            this.accountId = accountId;
            this.created = created;
        }

        public Long getAccountId() {
            return accountId;
        }

        public boolean isCreated() {
            return created;
        }
    }
}
