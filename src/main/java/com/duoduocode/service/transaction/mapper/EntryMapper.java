package com.duoduocode.service.transaction.mapper;

import com.duoduocode.service.transaction.entity.Entry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 分录 Mapper 接口
 */
@Mapper
public interface EntryMapper {

    /**
     * 插入分录
     */
    int insert(Entry entry);

    /**
     * 根据ID查询分录
     */
    Entry selectById(@Param("id") Long id);

    /**
     * 根据交易ID查询分录列表
     */
    List<Entry> selectByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 根据账户ID查询分录列表
     */
    List<Entry> selectByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据分类ID和日期范围统计借方金额
     */
    BigDecimal sumDebitByCategoryIdAndDateRange(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 根据分类ID和日期范围统计贷方金额
     */
    BigDecimal sumCreditByCategoryIdAndDateRange(
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 根据交易ID统计借方金额
     */
    BigDecimal sumDebitByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 根据交易ID统计贷方金额
     */
    BigDecimal sumCreditByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 根据账户ID统计借方金额
     */
    BigDecimal sumDebitByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据账户ID统计贷方金额
     */
    BigDecimal sumCreditByAccountId(@Param("accountId") Long accountId);

    /**
     * 删除分录
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量插入分录
     */
    int batchInsert(@Param("list") List<Entry> entries);

    /**
     * 根据交易ID删除分录
     */
    int deleteByTransactionId(@Param("transactionId") Long transactionId);

    /**
     * 根据交易ID列表批量查询分录
     */
    List<Entry> selectByTransactionIds(@Param("transactionIds") List<Long> transactionIds);
}
