package com.duoduocode.service.budget.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * R003 批量结转请求
 */
@Data
public class BatchCarryoverDTO {

    private List<Item> items;

    @Data
    public static class Item {

        private Long categoryId;

        /** 来源月份 YYYY-MM（仅允许上一个月） */
        private String fromMonth;

        private BigDecimal amount;
    }
}
