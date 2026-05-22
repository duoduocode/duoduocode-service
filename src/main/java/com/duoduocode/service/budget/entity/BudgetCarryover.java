package com.duoduocode.service.budget.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预算结转记录实体�? * 对应 budget_carryover �? */
public class BudgetCarryover {

    /**
     * 结转ID
     */
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 来源月份 YYYY-MM
     */
    private String fromMonth;

    /**
     * 目标月份 YYYY-MM
     */
    private String toMonth;

    /**
     * 结转金额
     */
    private BigDecimal carryoverAmount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getFromMonth() {
        return fromMonth;
    }

    public void setFromMonth(String fromMonth) {
        this.fromMonth = fromMonth;
    }

    public String getToMonth() {
        return toMonth;
    }

    public void setToMonth(String toMonth) {
        this.toMonth = toMonth;
    }

    public BigDecimal getCarryoverAmount() {
        return carryoverAmount;
    }

    public void setCarryoverAmount(BigDecimal carryoverAmount) {
        this.carryoverAmount = carryoverAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
