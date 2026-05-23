package com.duoduocode.service.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 周期交易模板VO
 * 用于返回模板详情
 */
public class RecurringTemplateVO {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 类型：expense-支出, income-收入
     */
    private String type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 目标账户ID
     */
    private Long toAccountId;

    /**
     * 目标账户名称
     */
    private String toAccountName;

    /**
     * 描述
     */
    private String description;

    /**
     * 频率：daily-每天, weekly-每周, monthly-每月, yearly-每年
     */
    private String frequency;

    /**
     * 频率名称
     */
    private String frequencyName;

    /**
     * 周几（1-7，weekly专用）
     */
    private Integer dayOfWeek;

    /**
     * 每月几号（1-31，monthly专用）
     */
    private Integer dayOfMonth;

    /**
     * 每年几月（1-12，yearly专用）
     */
    private Integer monthOfYear;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 最大执行次数（可选）
     */
    private Integer maxCount;

    /**
     * 执行次数
     */
    private Integer executedCount;

    /**
     * 下次触发日期
     */
    private LocalDate nextTriggerDate;

    /**
     * 上次触发时间
     */
    private LocalDateTime lastTriggeredAt;

    /**
     * 软删除标记
     */
    private Integer isDeleted;

    /**
     * 状态：active-激活, paused-暂停
     */
    private String status;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (type != null) {
            switch (type) {
                case "expense":
                    this.typeName = "支出";
                    break;
                case "income":
                    this.typeName = "收入";
                    break;
                default:
                    this.typeName = "未知";
            }
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public String getToAccountName() {
        return toAccountName;
    }

    public void setToAccountName(String toAccountName) {
        this.toAccountName = toAccountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
        if (frequency != null) {
            switch (frequency) {
                case "daily":
                    this.frequencyName = "每天";
                    break;
                case "weekly":
                    this.frequencyName = "每周";
                    break;
                case "monthly":
                    this.frequencyName = "每月";
                    break;
                case "yearly":
                    this.frequencyName = "每年";
                    break;
                default:
                    this.frequencyName = "未知";
            }
        }
    }

    public String getFrequencyName() {
        return frequencyName;
    }

    public void setFrequencyName(String frequencyName) {
        this.frequencyName = frequencyName;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Integer getMonthOfYear() {
        return monthOfYear;
    }

    public void setMonthOfYear(Integer monthOfYear) {
        this.monthOfYear = monthOfYear;
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

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Integer getExecutedCount() {
        return executedCount;
    }

    public void setExecutedCount(Integer executedCount) {
        this.executedCount = executedCount;
    }

    public LocalDate getNextTriggerDate() {
        return nextTriggerDate;
    }

    public void setNextTriggerDate(LocalDate nextTriggerDate) {
        this.nextTriggerDate = nextTriggerDate;
    }

    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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