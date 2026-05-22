package com.duoduocode.service.investment.mapper;

import com.duoduocode.service.investment.entity.InvestmentValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投资市�?Mapper 接口
 */
@Mapper
public interface InvestmentValueMapper {

    /**
     * 插入市值记�?     */
    int insert(InvestmentValue investmentValue);

    /**
     * 根据ID更新市值记�?     */
    int updateById(InvestmentValue investmentValue);

    /**
     * 根据ID查询市值记�?     */
    InvestmentValue selectById(@Param("id") Long id);

    /**
     * 根据账户ID查询市值记录列�?     */
    List<InvestmentValue> selectByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据账户ID和日期范围查询市值记�?     */
    List<InvestmentValue> selectByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 查询账户最新市值记�?     */
    InvestmentValue selectLatestByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据账户ID和日期查询市值记�?     */
    InvestmentValue selectByAccountIdAndDate(
            @Param("accountId") Long accountId,
            @Param("recordDate") String recordDate);

    /**
     * 删除市值记�?     */
    int deleteById(@Param("id") Long id);

    /**
     * 计算账户总收�?     */
    BigDecimal calculateTotalProfit(@Param("accountId") Long accountId);

    /**
     * 计算账户最新收益率
     */
    BigDecimal calculateLatestProfitRate(@Param("accountId") Long accountId);
}
