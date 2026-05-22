package com.duoduocode.service.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收支报表VO
 */
public class IncomeExpenseReportVO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private List<MonthlyData> monthlyData;
    private List<CategoryData> topExpenseCategories;

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<MonthlyData> getMonthlyData() {
        return monthlyData;
    }

    public void setMonthlyData(List<MonthlyData> monthlyData) {
        this.monthlyData = monthlyData;
    }

    public List<CategoryData> getTopExpenseCategories() {
        return topExpenseCategories;
    }

    public void setTopExpenseCategories(List<CategoryData> topExpenseCategories) {
        this.topExpenseCategories = topExpenseCategories;
    }
}
