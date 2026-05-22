package com.duoduocode.service.recurring.mapper;

import com.duoduocode.service.recurring.entity.RecurringTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 周期交易模板 Mapper 接口
 */
@Mapper
public interface RecurringTemplateMapper {

    /**
     * 插入模板
     */
    int insert(RecurringTemplate template);

    /**
     * 根据ID更新模板
     */
    int updateById(RecurringTemplate template);

    /**
     * 根据ID删除模板
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID查询模板
     */
    RecurringTemplate selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询模板列表
     */
    List<RecurringTemplate> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询到期的模板列表
     */
    List<RecurringTemplate> selectDueTemplates(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 更新模板状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新执行次数和下次触发日期
     */
    int updateExecution(@Param("id") Long id,
                        @Param("executedCount") Integer executedCount,
                        @Param("nextTriggerDate") LocalDate nextTriggerDate,
                        @Param("lastTriggeredAt") java.time.LocalDateTime lastTriggeredAt);
}
