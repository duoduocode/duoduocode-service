package com.duoduocode.service.notification.dto;

import java.util.List;

/**
 * 标记已读请求 DTO
 * ids：单条/多条标已读；all=true：仅通知型全部已读
 */
public class NotificationReadDTO {

    private List<Long> ids;

    private Boolean all;

    // Getters and Setters

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public Boolean getAll() {
        return all;
    }

    public void setAll(Boolean all) {
        this.all = all;
    }
}
