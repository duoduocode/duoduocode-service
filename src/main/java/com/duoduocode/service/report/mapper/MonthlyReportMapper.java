package com.duoduocode.service.report.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.report.entity.MonthlyReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度财务报告快照 Mapper（R004）
 */
@Mapper
public interface MonthlyReportMapper {

    /**
     * 按用户+月份查询快照（幂等回读）
     */
    MonthlyReport selectByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);

    /**
     * 按ID查询快照
     */
    MonthlyReport selectById(@Param("id") Long id);

    /**
     * 查询用户自指定月份起的全部快照（月份倒序）
     */
    List<MonthlyReport> selectByUserIdFromMonth(@Param("userId") Long userId,
                                                @Param("fromMonth") String fromMonth);

    /**
     * 插入快照（uk_user_month 冲突时抛 DuplicateKeyException）
     */
    int insert(MonthlyReport report);

    /**
     * 查询指定时点（含当日）的账户余额列表
     * 与 AccountMapper 动态余额同一分录聚合口径，仅追加 t.date <= asOfDate 过滤
     */
    List<Account> selectAccountBalancesAsOf(@Param("userId") Long userId, @Param("asOfDate") String asOfDate);

    /**
     * 统计月份内订阅扣款合计（source_template_id 非空的 expense 交易）
     */
    BigDecimal selectSubscriptionSpend(@Param("userId") Long userId,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate);

    /**
     * 查询指定月份有交易的全部用户ID（月报调度圈选）
     */
    List<Long> selectUserIdsWithTransactions(@Param("month") String month);
}
