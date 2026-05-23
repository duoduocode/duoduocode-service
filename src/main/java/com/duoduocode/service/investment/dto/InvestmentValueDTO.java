package com.duoduocode.service.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 投资市值数据传输对象
 * 用于记录市值
 */
public class InvestmentValueDTO {

    /**
     * 记录日期
     */
    private LocalDate date;

    /**
     * 市值
     */
    private BigDecimal marketValue;

    /**
     * 成本
     */
    private BigDecimal costBasis;

    /**
     * 备注
     */
    private String note;

    // Getters and Setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}