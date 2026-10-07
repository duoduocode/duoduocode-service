package com.duoduocode.service.report.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度财务报告快照实体类
 * 对应 monthly_report 表（R004）
 */
public class MonthlyReport {

    /**
     * 月报ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 报告月份 YYYY-MM
     */
    private String month;

    /**
     * 月总收入
     */
    private BigDecimal totalIncome;

    /**
     * 月总支出
     */
    private BigDecimal totalExpense;

    /**
     * 储蓄率%（收入为0时NULL）
     */
    private BigDecimal savingsRate;

    /**
     * 期初净资产（报告月1日时点）
     */
    private BigDecimal netWorthStart;

    /**
     * 期末净资产（报告月末时点）
     */
    private BigDecimal netWorthEnd;

    /**
     * 报告月订阅扣款合计
     */
    private BigDecimal subscriptionSpend;

    /**
     * 归因/预算执行/分类top快照JSON
     */
    private String detail;

    /**
     * 生成时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

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

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public BigDecimal getNetWorthStart() {
        return netWorthStart;
    }

    public void setNetWorthStart(BigDecimal netWorthStart) {
        this.netWorthStart = netWorthStart;
    }

    public BigDecimal getNetWorthEnd() {
        return netWorthEnd;
    }

    public void setNetWorthEnd(BigDecimal netWorthEnd) {
        this.netWorthEnd = netWorthEnd;
    }

    public BigDecimal getSubscriptionSpend() {
        return subscriptionSpend;
    }

    public void setSubscriptionSpend(BigDecimal subscriptionSpend) {
        this.subscriptionSpend = subscriptionSpend;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
