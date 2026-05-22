package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.BudgetCarryover;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预算结转 Mapper 接口
 */
@Mapper
public interface BudgetCarryoverMapper {

    /**
     * 插入结转记录
     */
    int insert(BudgetCarryover budgetCarryover);

    /**
     * 根据分类ID和目标月份查询结转记�?     */
    BudgetCarryover selectByCategoryIdAndToMonth(@Param("categoryId") Long categoryId, @Param("toMonth") String toMonth);

    /**
     * 根据分类ID和来源月份查询结转记�?     */
    BudgetCarryover selectByCategoryIdAndFromMonth(@Param("categoryId") Long categoryId, @Param("fromMonth") String fromMonth);

    /**
     * 根据目标月份查询结转记录列表
     */
    List<BudgetCarryover> selectByToMonth(@Param("toMonth") String toMonth);

    /**
     * 计算某分类某月份的总结转金�?     */
    BigDecimal sumCarryoverAmountByCategoryIdAndToMonth(@Param("categoryId") Long categoryId, @Param("toMonth") String toMonth);

    /**
     * 根据ID删除结转记录
     */
    int deleteById(@Param("id") Long id);
}
