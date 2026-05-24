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
}
