package com.duoduocode.service.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 理财收益视图对象
 * 用于返回收益记录
 */
public class InvestmentIncomeVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 收益日期
     */
    private LocalDate date;

    /**
     * 收益金额
     */
    private BigDecimal amount;

    /**
     * 收益类型：daily-每日收益 dividend-分红 maturity-到期收益
     */
    private String type;

    /**
     * 收益类型名称
     */
    private String typeName;

    /**
     * 备注
     */
    private String note;

    /**
     * 是否再投资：0-否 1-是
     */
    private Integer isReinvested;

    /**
     * 关联交易ID（可选）
     */
    private Long transactionId;

    /**
     * 创建时间
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        // 自动设置类型名称
        if (type != null) {
            switch (type) {
                case "daily":
                    this.typeName = "每日收益";
                    break;
                case "dividend":
                    this.typeName = "分红";
                    break;
                case "maturity":
                    this.typeName = "到期收益";
                    break;
                default:
                    this.typeName = "未知类型";
            }
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getIsReinvested() {
        return isReinvested;
    }

    public void setIsReinvested(Integer isReinvested) {
        this.isReinvested = isReinvested;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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