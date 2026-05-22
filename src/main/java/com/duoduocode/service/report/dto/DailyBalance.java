package com.duoduocode.service.report.dto;

import java.math.BigDecimal;

/**
 * 每日余额DTO
 */
public class DailyBalance {
    private String date;
    private BigDecimal balance;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
