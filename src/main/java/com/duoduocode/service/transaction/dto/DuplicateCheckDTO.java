package com.duoduocode.service.transaction.dto;

import java.math.BigDecimal;

/**
 * 重复交易检测数据传输对象
 */
public class DuplicateCheckDTO {

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易日期
     */
    private String date;

    /**
     * 交易描述
     */
    private String description;

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
}
