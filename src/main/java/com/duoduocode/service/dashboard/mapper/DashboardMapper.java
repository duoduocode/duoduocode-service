package com.duoduocode.service.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Mapper
 */
@Mapper
public interface DashboardMapper {

    /**
     * 获取上月净资产
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @return 上月净资产
     */
    BigDecimal getLastMonthNetWorth(@Param("userId") Long userId, @Param("startDate") String startDate);

    /**
     * 获取月度收支汇总
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收支汇总 (totalIncome, totalExpense, incomeCount, expenseCount)
     */
    Map<String, Object> getMonthlySummary(@Param("userId") Long userId,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    /**
     * 获取分类月度支出
     *
     * @param userId    用户ID
     * @param categoryId 分类ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 支出金额
     */
    BigDecimal getCategoryExpense(@Param("userId") Long userId,
                                   @Param("categoryId") Long categoryId,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate);

    /**
     * 获取分类支出列表（按金额排序）
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param limit     返回数量
     * @return 分类支出列表
     */
    List<Map<String, Object>> getTopExpenseCategories(@Param("userId") Long userId,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate,
                                                       @Param("limit") Integer limit);

    /**
     * 获取指定日期范围内的支出
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 支出金额
     */
    BigDecimal getExpenseByDateRange(@Param("userId") Long userId,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate);

    /**
     * 获取交易分类信息
     *
     * @param transactionId 交易ID
     * @return 分类信息 (name, icon, type)
     */
    Map<String, Object> getTransactionCategory(@Param("transactionId") Long transactionId);
}
