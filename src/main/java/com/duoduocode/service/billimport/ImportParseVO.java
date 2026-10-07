package com.duoduocode.service.billimport;

import java.math.BigDecimal;
import java.util.List;

/**
 * 导入预览 VO（无状态解析结果 + 判重标记 + 新账户清单）
 */
public class ImportParseVO {

    private String source;

    private Integer totalRows;

    private Integer importableCount;

    private Integer duplicateCount;

    private Integer ignoredCount;

    private List<RowVO> rows;

    private List<String> newAccounts;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getImportableCount() {
        return importableCount;
    }

    public void setImportableCount(Integer importableCount) {
        this.importableCount = importableCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public Integer getIgnoredCount() {
        return ignoredCount;
    }

    public void setIgnoredCount(Integer ignoredCount) {
        this.ignoredCount = ignoredCount;
    }

    public List<RowVO> getRows() {
        return rows;
    }

    public void setRows(List<RowVO> rows) {
        this.rows = rows;
    }

    public List<String> getNewAccounts() {
        return newAccounts;
    }

    public void setNewAccounts(List<String> newAccounts) {
        this.newAccounts = newAccounts;
    }

    /** 行状态明细（上限 500 行展示） */
    public static class RowVO {

        private Integer lineNo;

        private String date;

        private String time;

        private String description;

        private String counterparty;

        private BigDecimal amount;

        /**
         * expense/income/other
         */
        private String direction;

        private String categoryName;

        private String payMethod;

        private String accountName;

        private String orderNo;

        /**
         * importable/duplicate/ignored
         */
        private String status;

        private String reason;

        public Integer getLineNo() {
            return lineNo;
        }

        public void setLineNo(Integer lineNo) {
            this.lineNo = lineNo;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCounterparty() {
            return counterparty;
        }

        public void setCounterparty(String counterparty) {
            this.counterparty = counterparty;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getPayMethod() {
            return payMethod;
        }

        public void setPayMethod(String payMethod) {
            this.payMethod = payMethod;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
