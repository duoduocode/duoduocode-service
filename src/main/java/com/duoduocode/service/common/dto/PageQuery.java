package com.duoduocode.service.common.dto;

/**
 * 分页查询参数
 */
public class PageQuery {

    /**
     * 当前页码，默认第1页
     */
    private Integer page = 1;

    /**
     * 每页条数，默认20条
     */
    private Integer pageSize = 20;

    /**
     * 开始日期（可选）
     */
    private String startDate;

    /**
     * 结束日期（可选）
     */
    private String endDate;

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

    /**
     * 计算偏移量
     */
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
