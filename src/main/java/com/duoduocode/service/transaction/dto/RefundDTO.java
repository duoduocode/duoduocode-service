package com.duoduocode.service.transaction.dto;

import java.math.BigDecimal;

/**
 * 退款数据传输对象
 */
public class RefundDTO {

    /**
     * 退款金额
     */
    private BigDecimal amount;

    /**
     * 退款日期
     */
    private String date;

    /**
     * 退款说明
     */
    private String description;

    /**
     * 退款入账账户
     */
    private Long accountId;

    // Getters and Setters

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
