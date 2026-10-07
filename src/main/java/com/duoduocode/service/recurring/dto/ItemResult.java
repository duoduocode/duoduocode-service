package com.duoduocode.service.recurring.dto;

import lombok.Getter;

/**
 * 批量操作单项结果
 */
@Getter
public class ItemResult {

    private final Long id;

    private final boolean success;

    private final String reason;

    private ItemResult(Long id, boolean success, String reason) {
        this.id = id;
        this.success = success;
        this.reason = reason;
    }

    public static ItemResult ok(Long id) {
        return new ItemResult(id, true, null);
    }

    public static ItemResult fail(Long id, String reason) {
        return new ItemResult(id, false, reason);
    }
}
