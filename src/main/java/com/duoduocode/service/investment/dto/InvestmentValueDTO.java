package com.duoduocode.service.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 投资市值数据传输对�? * 用于记录市�? */
public class InvestmentValueDTO {

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 市�?     */
    private BigDecimal marketValue;

    /**
     * 成本
     */
    private BigDecimal costBasis;

    /**
     * 备注
     */
    private String remark;

    // Getters and Setters

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
