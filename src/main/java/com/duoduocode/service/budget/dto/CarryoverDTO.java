package com.duoduocode.service.budget.dto;

import java.math.BigDecimal;

/**
 * 预算结转数据传输对象
 */
public class CarryoverDTO {

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

    // Getters and Setters

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
}
