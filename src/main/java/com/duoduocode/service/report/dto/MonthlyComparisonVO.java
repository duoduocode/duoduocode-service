package com.duoduocode.service.report.dto;

import java.math.BigDecimal;

/**
 * 月度对比VO
 */
public class MonthlyComparisonVO {
    private String month1;
    private String month2;
    private BigDecimal income1;
    private BigDecimal income2;
    private BigDecimal incomeChange;
    private BigDecimal expense1;
    private BigDecimal expense2;
    private BigDecimal expenseChange;

    public String getMonth1() {
        return month1;
    }

    public void setMonth1(String month1) {
        this.month1 = month1;
    }

    public String getMonth2() {
        return month2;
    }

    public void setMonth2(String month2) {
        this.month2 = month2;
    }

    public BigDecimal getIncome1() {
        return income1;
    }

    public void setIncome1(BigDecimal income1) {
        this.income1 = income1;
    }

    public BigDecimal getIncome2() {
        return income2;
    }

    public void setIncome2(BigDecimal income2) {
        this.income2 = income2;
    }

    public BigDecimal getIncomeChange() {
        return incomeChange;
    }

    public void setIncomeChange(BigDecimal incomeChange) {
        this.incomeChange = incomeChange;
    }

    public BigDecimal getExpense1() {
        return expense1;
    }

    public void setExpense1(BigDecimal expense1) {
        this.expense1 = expense1;
    }

    public BigDecimal getExpense2() {
        return expense2;
    }

    public void setExpense2(BigDecimal expense2) {
        this.expense2 = expense2;
    }

    public BigDecimal getExpenseChange() {
        return expenseChange;
    }

    public void setExpenseChange(BigDecimal expenseChange) {
        this.expenseChange = expenseChange;
    }
}
