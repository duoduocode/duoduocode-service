package com.duoduocode.service.transaction.dto;

import com.duoduocode.service.common.dto.PageQuery;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易搜索查询参数
 * 支持多条件组合筛选
 */
public class TransactionSearchQuery extends PageQuery {

    /**
     * 关键词（描述模糊搜索）
     */
    private String keyword;

    /**
     * 最小金额
     */
    private BigDecimal minAmount;

    /**
     * 最大金额
     */
    private BigDecimal maxAmount;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 标签ID列表
     */
    private List<Long> tagIds;

    /**
     * 交易类型：expense-支出, income-收入, transfer-转账
     */
    private String type;

    /**
     * 排序字段：date-日期, amount-金额, created_at-创建时间
     */
    private String sortBy = "date";

    /**
     * 排序方向：asc-升序, desc-降序
     */
    private String sortOrder = "desc";

    // Getters and Setters

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
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

    @Override
    public String getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Override
    public String getEndDate() {
        return endDate;
    }

    @Override
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
