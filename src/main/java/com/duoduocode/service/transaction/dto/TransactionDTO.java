package com.duoduocode.service.transaction.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 交易数据传输对象
 */
public class TransactionDTO {

    /**
     * 交易日期
     */
    private LocalDate date;

    /**
     * 交易时间
     */
    private LocalTime time;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 交易类型：expense-支出, income-收入, transfer-转账, repayment-还款
     */
    private String transactionType;

    /**
     * 账户ID（转账时为源账户）
     */
    @JsonAlias("fromAccountId")
    private Long accountId;

    /**
     * 目标账户ID（转账/还款用）
     */
    @JsonAlias("toAccountId")
    private Long targetAccountId;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 记账模式：simple-简化模式, full-完整模式
     */
    private String mode = "simple";

    /**
     * 分录列表（完整模式用）
     */
    private List<EntryDTO> entries;

    /**
     * 待办落账溯源：来源周期模板ID（落账后自动推进模板并消化待办）
     */
    private Long sourceTemplateId;

    /**
     * 导入溯源：来源账单交易单号
     */
    private String externalOrderNo;

    /**
     * 导入溯源：导入批次ID（服务端注入，客户端不传）
     */
    private Long importBatchId;

    // Getters and Setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(Long targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<EntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryDTO> entries) {
        this.entries = entries;
    }

    public Long getSourceTemplateId() {
        return sourceTemplateId;
    }

    public void setSourceTemplateId(Long sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId;
    }

    public String getExternalOrderNo() {
        return externalOrderNo;
    }

    public void setExternalOrderNo(String externalOrderNo) {
        this.externalOrderNo = externalOrderNo;
    }

    public Long getImportBatchId() {
        return importBatchId;
    }

    public void setImportBatchId(Long importBatchId) {
        this.importBatchId = importBatchId;
    }
}
