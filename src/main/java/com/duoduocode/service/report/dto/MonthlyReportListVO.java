package com.duoduocode.service.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度财务报告列表 VO（R004）
 * 近 N 个完整月倒序（不含当月），已生成/未生成两态行
 */
public class MonthlyReportListVO {

    private List<Row> rows;

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    /**
     * 月报列表行
     */
    public static class Row {

        /**
         * 月报ID（已生成时非空，跳详情用）
         */
        private Long id;

        /**
         * 月份 YYYY-MM
         */
        private String month;

        /**
         * 是否已生成快照
         */
        private Boolean generated;

        /**
         * 净资产变动（未生成时 null）
         */
        private BigDecimal netWorthChange;

        /**
         * 储蓄率%（未生成时 null）
         */
        private BigDecimal savingsRate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Boolean getGenerated() {
            return generated;
        }

        public void setGenerated(Boolean generated) {
            this.generated = generated;
        }

        public BigDecimal getNetWorthChange() {
            return netWorthChange;
        }

        public void setNetWorthChange(BigDecimal netWorthChange) {
            this.netWorthChange = netWorthChange;
        }

        public BigDecimal getSavingsRate() {
            return savingsRate;
        }

        public void setSavingsRate(BigDecimal savingsRate) {
            this.savingsRate = savingsRate;
        }
    }
}
