package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.SpecialBudget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 专项预算 Mapper 接口
 */
@Mapper
public interface SpecialBudgetMapper {

    /**
     * 插入专项预算
     */
    int insert(SpecialBudget specialBudget);

    /**
     * 根据ID更新专项预算
     */
    int updateById(SpecialBudget specialBudget);

    /**
     * 根据ID查询专项预算
     */
    SpecialBudget selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询专项预算列表
     */
    List<SpecialBudget> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户进行中的专项预算
     */
    List<SpecialBudget> selectOngoingByUserId(@Param("userId") Long userId);

    /**
     * 更新状态为已完�?     */
    int updateStatusToCompleted(@Param("id") Long id, @Param("actualAmount") BigDecimal actualAmount);

    /**
     * 删除专项预算
     */
    int deleteById(@Param("id") Long id);
}
