package com.duoduocode.service.report.dto;

import java.math.BigDecimal;

/**
 * 月度数据DTO
 */
public class MonthlyData {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }
}
