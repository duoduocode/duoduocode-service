package com.duoduocode.service.budget.dto;

import java.math.BigDecimal;

/**
 * 日常预算视图对象
 */
public class DailyBudgetVO {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类图标
     */
    private String categoryIcon;

    /**
     * 分类颜色
     */
    private String categoryColor;

    /**
     * 月度预算
     */
    private BigDecimal monthlyBudget;

    /**
     * 周度预算
     */
    private BigDecimal weeklyBudget;

    /**
     * 预警阈�?     */
    private BigDecimal alertThreshold;

    /**
     * 结转金额
     */
    private BigDecimal carryoverAmount;

    /**
     * 可用预算
     */
    private BigDecimal availableBudget;

    /**
     * 已使用金�?     */
    private BigDecimal usedAmount;

    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;

    /**
     * 使用百分�?     */
    private BigDecimal usagePercent;

    /**
     * 状态：normal-正常 warning-预警 exceeded-超支
     */
    private String status;

    // Getters and Setters

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
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

    public BigDecimal getCarryoverAmount() {
        return carryoverAmount;
    }

    public void setCarryoverAmount(BigDecimal carryoverAmount) {
        this.carryoverAmount = carryoverAmount;
    }

    public BigDecimal getAvailableBudget() {
        return availableBudget;
    }

    public void setAvailableBudget(BigDecimal availableBudget) {
        this.availableBudget = availableBudget;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getUsagePercent() {
        return usagePercent;
    }

    public void setUsagePercent(BigDecimal usagePercent) {
        this.usagePercent = usagePercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
