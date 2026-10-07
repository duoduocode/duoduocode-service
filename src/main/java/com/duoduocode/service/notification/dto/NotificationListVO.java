package com.duoduocode.service.notification.dto;

import java.util.List;
import java.util.Map;

/**
 * 消息列表分页 VO
 */
public class NotificationListVO {

    private List<NotificationVO> items;

    private Long total;

    private Integer page;

    private Integer pageSize;

    /**
     * 未读数：{todo, notice, total}
     */
    private Map<String, Long> unreadCount;

    // Getters and Setters

    public List<NotificationVO> getItems() {
        return items;
    }

    public void setItems(List<NotificationVO> items) {
        this.items = items;
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

    public Map<String, Long> getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Map<String, Long> unreadCount) {
        this.unreadCount = unreadCount;
    }
}
