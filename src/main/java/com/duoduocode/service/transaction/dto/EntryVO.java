package com.duoduocode.service.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分录视图对象
 */
public class EntryVO {

    /**
     * 分录ID
     */
    private Long id;

    /**
     * 关联交易ID
     */
    private Long transactionId;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 账户图标
     */
    private String accountIcon;

    /**
     * 借方金额
     */
    private BigDecimal debit;

    /**
     * 贷方金额
     */
    private BigDecimal credit;

    /**
     * 账户类型
     */
    private String accountType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountIcon() {
        return accountIcon;
    }

    public void setAccountIcon(String accountIcon) {
        this.accountIcon = accountIcon;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
