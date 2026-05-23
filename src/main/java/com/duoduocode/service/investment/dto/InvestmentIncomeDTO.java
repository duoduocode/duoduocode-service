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
    private LocalDate date;

    /**
     * 收益金额
     */
    private BigDecimal amount;

    /**
     * 收益类型：daily-每日收益 dividend-分红 maturity-到期收益
     */
    private String type;

    /**
     * 备注
     */
    private String note;

    /**
     * 是否再投资：0-否 1-是
     */
    private Integer isReinvested;

    /**
     * 关联交易ID（可选）
     */
    private Long transactionId;

    // Getters and Setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getIsReinvested() {
        return isReinvested;
    }

    public void setIsReinvested(Integer isReinvested) {
        this.isReinvested = isReinvested;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }
}