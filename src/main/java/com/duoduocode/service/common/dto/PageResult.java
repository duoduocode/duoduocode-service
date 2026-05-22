package com.duoduocode.service.common.dto;

import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 数据类型
 */
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
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
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> list, Integer page, Integer pageSize, boolean hasMore) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
