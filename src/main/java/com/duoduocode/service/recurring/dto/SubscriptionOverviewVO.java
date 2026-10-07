package com.duoduocode.service.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 订阅管理总览 VO（R004）
 * 口径：status=active && type=expense && amount>0；折算 monthly=原额 / weekly=×52÷12 / daily=×365÷12 / yearly=÷12
 */
public class SubscriptionOverviewVO {

    /**
     * 月度折算合计（HALF_UP 2 位）
     */
    private BigDecimal monthlyTotal = BigDecimal.ZERO;

    /**
     * 订阅项数
     */
    private Integer itemCount = 0;

    /**
     * 近 30 天（含端点）计划扣款笔数
     */
    private Integer next30DaysCount = 0;

    /**
     * 近 30 天（含端点）计划扣款金额合计（按模板当前金额）
     */
    private BigDecimal next30DaysAmount = BigDecimal.ZERO;

    /**
     * 订阅清单（按折算月额降序）
     */
    private List<Item> items = new ArrayList<>();

    public BigDecimal getMonthlyTotal() {
        return monthlyTotal;
    }

    public void setMonthlyTotal(BigDecimal monthlyTotal) {
        this.monthlyTotal = monthlyTotal;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getNext30DaysCount() {
        return next30DaysCount;
    }

    public void setNext30DaysCount(Integer next30DaysCount) {
        this.next30DaysCount = next30DaysCount;
    }

    public BigDecimal getNext30DaysAmount() {
        return next30DaysAmount;
    }

    public void setNext30DaysAmount(BigDecimal next30DaysAmount) {
        this.next30DaysAmount = next30DaysAmount;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * 单条订阅项：折算月额 + 涨价三态
     */
    public static class Item {

        private Long templateId;

        private String name;

        /**
         * 模板当前金额
         */
        private BigDecimal amount;

        private String frequency;

        /**
         * 折算月额（HALF_UP 2 位）
         */
        private BigDecimal monthlyAmount;

        /**
         * 模板下次触发日
         */
        private LocalDate nextTriggerDate;

        /**
         * 该模板最近一笔交易金额（无基准时 null）
         */
        private BigDecimal lastPaidAmount;

        /**
         * amount > lastPaidAmount（lastPaidAmount=null 时恒 false）
         */
        private Boolean priceIncreased = false;

        /**
         * 涨价差额（涨价时非空）
         */
        private BigDecimal increaseAmount;

        /**
         * 涨价幅度 %（涨价时非空，1 位小数）
         */
        private BigDecimal increasePercent;

        public Long getTemplateId() {
            return templateId;
        }

        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public BigDecimal getMonthlyAmount() {
            return monthlyAmount;
        }

        public void setMonthlyAmount(BigDecimal monthlyAmount) {
            this.monthlyAmount = monthlyAmount;
        }

        public LocalDate getNextTriggerDate() {
            return nextTriggerDate;
        }

        public void setNextTriggerDate(LocalDate nextTriggerDate) {
            this.nextTriggerDate = nextTriggerDate;
        }

        public BigDecimal getLastPaidAmount() {
            return lastPaidAmount;
        }

        public void setLastPaidAmount(BigDecimal lastPaidAmount) {
            this.lastPaidAmount = lastPaidAmount;
        }

        public Boolean getPriceIncreased() {
            return priceIncreased;
        }

        public void setPriceIncreased(Boolean priceIncreased) {
            this.priceIncreased = priceIncreased;
        }

        public BigDecimal getIncreaseAmount() {
            return increaseAmount;
        }

        public void setIncreaseAmount(BigDecimal increaseAmount) {
            this.increaseAmount = increaseAmount;
        }

        public BigDecimal getIncreasePercent() {
            return increasePercent;
        }

        public void setIncreasePercent(BigDecimal increasePercent) {
            this.increasePercent = increasePercent;
        }
    }
}
