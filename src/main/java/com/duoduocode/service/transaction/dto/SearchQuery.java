package com.duoduocode.service.transaction.dto;

/**
 * 搜索查询参数
 */
public class SearchQuery {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 最小金额
     */
    private String minAmount;

    /**
     * 最大金额
     */
    private String maxAmount;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 当前页码
     */
    private Integer page = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 20;

    // Getters and Setters

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(String minAmount) {
        this.minAmount = minAmount;
    }

    public String getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(String maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 计算偏移量
     */
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
