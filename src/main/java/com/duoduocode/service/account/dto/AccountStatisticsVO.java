package com.duoduocode.service.account.dto;

import java.math.BigDecimal;

public class AccountStatisticsVO {

    private BigDecimal income;
    private BigDecimal expense;
    private Long transactionCount;

    public AccountStatisticsVO() {
    }

    public AccountStatisticsVO(BigDecimal income, BigDecimal expense, Long transactionCount) {
        this.income = income;
        this.expense = expense;
        this.transactionCount = transactionCount;
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

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }
}
