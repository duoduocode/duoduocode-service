package com.duoduocode.service.dashboard.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 首页数据看板 DTO
 */
@Data
public class DashboardVO {

    // ==================== 账户汇总 ====================
    /** 总资产 */
    private BigDecimal totalAssets;

    /** 总负债 */
    private BigDecimal totalLiabilities;

    /** 总投资 */
    private BigDecimal totalInvestments;

    /** 净资产 */
    private BigDecimal netWorth;

    /** 上月净资产 */
    private BigDecimal lastMonthNetWorth;

    /** 净资产变化 */
    private BigDecimal netWorthChange;

    /** 净资产变化百分比 */
    private BigDecimal netWorthChangePercent;

    // ==================== 月度收支 ====================
    /** 本月收入 */
    private BigDecimal totalIncome;

    /** 本月支出 */
    private BigDecimal totalExpense;

    /** 本月净收支 */
    private BigDecimal netAmount;

    /** 收入笔数 */
    private Integer incomeCount;

    /** 支出笔数 */
    private Integer expenseCount;

    // ==================== 分类支出TOP5 ====================
    /** 支出TOP5分类 */
    private List<CategoryExpenseVO> topExpenseCategories;

    // ==================== 今日/本周数据 ====================
    /** 今日支出 */
    private BigDecimal todayExpense;

    /** 本周支出 */
    private BigDecimal weekExpense;

    // ==================== 其他 ====================
    /** 账户数量 */
    private Integer accountCount;

    /** 本月交易笔数 */
    private Integer monthTransactionCount;

    /**
     * 分类支出 VO
     */
    @Data
    public static class CategoryExpenseVO {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer transactionCount;
    }
}
