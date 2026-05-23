package com.duoduocode.service.investment.mapper;

import com.duoduocode.service.investment.entity.InvestmentIncome;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 理财收益 Mapper 接口
 */
@Mapper
public interface InvestmentIncomeMapper {

    /**
     * 插入收益记录
     */
    int insert(InvestmentIncome investmentIncome);

    /**
     * 根据ID更新收益记录
     */
    int updateById(InvestmentIncome investmentIncome);

    /**
     * 根据ID查询收益记录
     */
    InvestmentIncome selectById(@Param("id") Long id);

    /**
     * 根据账户ID查询收益记录列表
     */
    List<InvestmentIncome> selectByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据账户ID和日期范围查询收益记�?     */
    List<InvestmentIncome> selectByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 根据账户ID和月份查询收益记�?     */
    List<InvestmentIncome> selectByAccountIdAndMonth(
            @Param("accountId") Long accountId,
            @Param("month") String month);

    /**
     * 计算账户总收�?     */
    BigDecimal calculateTotalIncome(@Param("accountId") Long accountId);

    /**
     * 计算账户某月总收�?     */
    BigDecimal calculateMonthlyIncome(
            @Param("accountId") Long accountId,
            @Param("month") String month);

    /**
     * 根据收益类型统计收益
     */
    BigDecimal calculateIncomeByType(
            @Param("accountId") Long accountId,
            @Param("type") String type);

    /**
     * 删除收益记录
     */
    int deleteById(@Param("id") Long id);
}
