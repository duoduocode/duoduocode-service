package com.duoduocode.service.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 专项预算数据传输对象
 */
public class SpecialBudgetDTO {

    /**
     * 预算名称
     */
    private String name;

    /**
     * 预算总额
     */
    private BigDecimal totalAmount;

    /**
     * 开始日�?     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 备注
     */
    private String note;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
