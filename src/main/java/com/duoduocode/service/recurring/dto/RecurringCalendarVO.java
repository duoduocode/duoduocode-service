package com.duoduocode.service.recurring.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 周期交易月度日历视图
 */
@Data
public class RecurringCalendarVO {

    /** YYYY-MM */
    private String month;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {

        private Long templateId;

        private String name;

        private BigDecimal amount;

        private String type;

        private String frequency;

        private LocalDate date;

        /** done-已确认 due-已到期待确认 planned-计划中 */
        private String status;
    }
}
