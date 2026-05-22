package com.duoduocode.service.transaction.dto;

import java.math.BigDecimal;

/**
 * 分录数据传输对象
 */
public class EntryDTO {

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 借方金额
     */
    private BigDecimal debit;

    /**
     * 贷方金额
     */
    private BigDecimal credit;

    /**
     * 账户类型：category-分类, account-账户
     */
    private String accountType;

    // Getters and Setters

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
