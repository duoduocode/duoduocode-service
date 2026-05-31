package com.duoduocode.service.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体�? * 对应 account �? */
public class Account {

    /**
     * 账户ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 账户名称
     */
    private String name;

    /**
     * 账户类型：asset-资产 liability-负�?investment-投资
     */
    private String type;

    /**
     * 图标emoji
     */
    private String icon;

    /**
     * 颜色（十六进制）
     */
    private String color;

    /**
     * 初始余额
     */
    private BigDecimal initialBalance;

    /**
     * 信用额度（负债类账户专用�?     */
    private BigDecimal creditLimit;

    /**
     * 是否计入净资产
     */
    private Boolean includeInNetWorth;

    /**
     * 是否允许转账
     */
    private Boolean allowTransfer;

    /**
     * 是否开启余额预�?     */
    private Boolean enableAlert;

    /**
     * 预警阈�?     */
    private BigDecimal alertThreshold;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 账户描述
     */
    private String desc;

    /**
     * 软删除标�     */
    private Boolean isDeleted;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Boolean getIncludeInNetWorth() {
        return includeInNetWorth;
    }

    public void setIncludeInNetWorth(Boolean includeInNetWorth) {
        this.includeInNetWorth = includeInNetWorth;
    }

    public Boolean getAllowTransfer() {
        return allowTransfer;
    }

    public void setAllowTransfer(Boolean allowTransfer) {
        this.allowTransfer = allowTransfer;
    }

    public Boolean getEnableAlert() {
        return enableAlert;
    }

    public void setEnableAlert(Boolean enableAlert) {
        this.enableAlert = enableAlert;
    }

    public BigDecimal getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(BigDecimal alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
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
