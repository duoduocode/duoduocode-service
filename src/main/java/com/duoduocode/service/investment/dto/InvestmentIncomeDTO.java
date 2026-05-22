package com.duoduocode.service.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理财收益数据传输对象
 * 用于记录收益
 */
public class InvestmentIncomeDTO {

    /**
     * 收益日期
     */
    private LocalDate incomeDate;

    /**
     * 收益金额
     */
    private BigDecimal incomeAmount;

    /**
     * 收益类型：interest-利息 dividend-分红 capital_gain-资本利得 other-其他
     */
    private String incomeType;

    /**
     * 收益描述
     */
    private String description;

    /**
     * 关联交易ID（可选）
     */
    private Long transactionId;

    // Getters and Setters

    public LocalDate getIncomeDate() {
        return incomeDate;
    }

    public void setIncomeDate(LocalDate incomeDate) {
        this.incomeDate = incomeDate;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public void setIncomeAmount(BigDecimal incomeAmount) {
        this.incomeAmount = incomeAmount;
    }

    public String getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(String incomeType) {
        this.incomeType = incomeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}
