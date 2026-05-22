package com.duoduocode.service.budget.dto;

import java.math.BigDecimal;

/**
 * 日常预算数据传输对象
 */
public class DailyBudgetDTO {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 月度预算
     */
    private BigDecimal monthlyBudget;

    /**
     * 周度预算
     */
    private BigDecimal weeklyBudget;

    /**
     * 预警阈值（百分比）
     */
    private BigDecimal alertThreshold;

    // Getters and Setters

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getWeeklyBudget() {
        return weeklyBudget;
    }

    public void setWeeklyBudget(BigDecimal weeklyBudget) {
        this.weeklyBudget = weeklyBudget;
    }

    public BigDecimal getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
    }
}
