package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.DailyBudget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DailyBudgetMapper {

    int insert(DailyBudget budget);

    int updateById(DailyBudget budget);

    int upsert(DailyBudget budget);

    List<DailyBudget> selectByUserIdAndMonth(@Param("userId") Long userId, @Param("month") String month);

    DailyBudget selectByUserIdAndCategoryIdAndMonth(@Param("userId") Long userId,
                                                     @Param("categoryId") Long categoryId,
                                                     @Param("month") String month);

    /**
     * 跨用户按月查询预算（每日兜底扫描用）
     */
    List<DailyBudget> selectByMonth(@Param("month") String month);

    /**
     * 导出备份：按用户查询全部每日预算（全部月份）
     */
    List<DailyBudget> selectAllByUserId(@Param("userId") Long userId);
}
