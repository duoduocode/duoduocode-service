package com.duoduocode.service.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 月度财务报告详情 VO（R004）
 * 核心指标列 + detail JSON（归因/预算/分类top）合并视图
 */
public class MonthlyReportDetailVO {

    /**
     * 月报ID（通知 refId 同源）
     */
    private Long id;

    /**
     * 报告月份 YYYY-MM
     */
    private String month;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 月总收入
     */
    private BigDecimal totalIncome;

    /**
     * 月总支出
     */
    private BigDecimal totalExpense;

    /**
     * 结余 = 收入 − 支出
     */
    private BigDecimal balance;

    /**
     * 储蓄率%（收入为0时null，前端显示"—"）
     */
    private BigDecimal savingsRate;

    /**
     * 期初净资产（报告月1日时点）
     */
    private BigDecimal netWorthStart;

    /**
     * 期末净资产（报告月末时点）
     */
    private BigDecimal netWorthEnd;

    /**
     * 净资产变动 = 期末 − 期初
     */
    private BigDecimal netWorthChange;

    /**
     * 净资产归因 top（按变动绝对值降序）
     */
    private List<AccountChange> topAccountChanges;

    /**
     * 收入 top 分类
     */
    private CategoryTop incomeTop;

    /**
     * 支出 top 分类
     */
    private CategoryTop expenseTop;

    /**
     * 预算执行（未设预算时 null）
     */
    private BudgetExecution budget;

    /**
     * 报告月订阅实际扣款合计
     */
    private BigDecimal subscriptionSpend;

    // Getters and Setters

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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public BigDecimal getNetWorthStart() {
        return netWorthStart;
    }

    public void setNetWorthStart(BigDecimal netWorthStart) {
        this.netWorthStart = netWorthStart;
    }

    public BigDecimal getNetWorthEnd() {
        return netWorthEnd;
    }

    public void setNetWorthEnd(BigDecimal netWorthEnd) {
        this.netWorthEnd = netWorthEnd;
    }

    public BigDecimal getNetWorthChange() {
        return netWorthChange;
    }

    public void setNetWorthChange(BigDecimal netWorthChange) {
        this.netWorthChange = netWorthChange;
    }

    public List<AccountChange> getTopAccountChanges() {
        return topAccountChanges;
    }

    public void setTopAccountChanges(List<AccountChange> topAccountChanges) {
        this.topAccountChanges = topAccountChanges;
    }

    public CategoryTop getIncomeTop() {
        return incomeTop;
    }

    public void setIncomeTop(CategoryTop incomeTop) {
        this.incomeTop = incomeTop;
    }

    public CategoryTop getExpenseTop() {
        return expenseTop;
    }

    public void setExpenseTop(CategoryTop expenseTop) {
        this.expenseTop = expenseTop;
    }

    public BudgetExecution getBudget() {
        return budget;
    }

    public void setBudget(BudgetExecution budget) {
        this.budget = budget;
    }

    public BigDecimal getSubscriptionSpend() {
        return subscriptionSpend;
    }

    public void setSubscriptionSpend(BigDecimal subscriptionSpend) {
        this.subscriptionSpend = subscriptionSpend;
    }

    /**
     * 净资产归因行
     */
    public static class AccountChange {

        private Long accountId;
        private String name;
        private BigDecimal change;

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getChange() {
            return change;
        }

        public void setChange(BigDecimal change) {
            this.change = change;
        }
    }

    /**
     * 收支 top 分类
     */
    public static class CategoryTop {

        private Long categoryId;
        private String name;
        private BigDecimal amount;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
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
    }

    /**
     * 预算执行聚合
     */
    public static class BudgetExecution {

        /**
         * 总执行率%（已用 ÷ 预算 × 100）
         */
        private BigDecimal rate;

        private List<BudgetItem> items;

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public List<BudgetItem> getItems() {
            return items;
        }

        public void setItems(List<BudgetItem> items) {
            this.items = items;
        }
    }

    /**
     * 预算执行明细项
     */
    public static class BudgetItem {

        private Long categoryId;
        private String name;
        private BigDecimal spent;
        private BigDecimal budget;
        private BigDecimal usagePercent;
        private Boolean exceeded;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getSpent() {
            return spent;
        }

        public void setSpent(BigDecimal spent) {
            this.spent = spent;
        }

        public BigDecimal getBudget() {
            return budget;
        }

        public void setBudget(BigDecimal budget) {
            this.budget = budget;
        }

        public BigDecimal getUsagePercent() {
            return usagePercent;
        }

        public void setUsagePercent(BigDecimal usagePercent) {
            this.usagePercent = usagePercent;
        }

        public Boolean getExceeded() {
            return exceeded;
        }

        public void setExceeded(Boolean exceeded) {
            this.exceeded = exceeded;
        }
    }
}
