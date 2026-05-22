package com.duoduocode.service.report.mapper;

import com.duoduocode.service.report.dto.CategoryData;
import com.duoduocode.service.report.dto.DailyBalance;
import com.duoduocode.service.report.dto.MonthlyData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportMapper {

    /**
     * 查询月度收支数据
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月度收支数据列表
     */
    List<MonthlyData> selectMonthlyIncomeExpense(@Param("userId") Long userId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    /**
     * 查询分类统计数据
     * @param userId 用户ID
     * @param type 类型(income/expense)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类统计数据列表
     */
    List<CategoryData> selectCategoryStats(@Param("userId") Long userId,
                                            @Param("type") String type,
                                            @Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    /**
     * 查询账户每日余额
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日余额数据列表
     */
    List<DailyBalance> selectAccountDailyBalance(@Param("accountId") Long accountId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);
}
