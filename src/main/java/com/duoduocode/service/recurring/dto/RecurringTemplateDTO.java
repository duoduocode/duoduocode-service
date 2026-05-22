package com.duoduocode.service.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 周期交易模板DTO
 * 用于创建和更新模板
 */
public class RecurringTemplateDTO {

    /**
     * 模板名称
     */
    private String name;

    /**
     * 交易类型：expense-支出, income-收入, transfer-转账
     */
    private String type;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 目标账户ID（转账时使用）
     */
    private Long toAccountId;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 频率：daily-每日, weekly-每周, monthly-每月, yearly-每年
     */
    private String frequency;

    /**
     * 星期几（1-7，weekly时使用）
     */
    private Integer dayOfWeek;

    /**
     * 每月几号（1-31，monthly时使用）
     */
    private Integer dayOfMonth;

    /**
     * 每年几月（1-12，yearly时使用）
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
     * 最大执行次数
     */
    private Integer maxCount;

    // Getters and Setters

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
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
}
