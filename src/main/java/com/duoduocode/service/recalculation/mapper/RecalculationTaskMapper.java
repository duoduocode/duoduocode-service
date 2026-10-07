package com.duoduocode.service.recalculation.mapper;

import com.duoduocode.service.recalculation.entity.RecalculationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量誊录异步任务 Mapper
 */
@Mapper
public interface RecalculationTaskMapper {

    int insert(RecalculationTask task);

    RecalculationTask selectById(@Param("id") Long id);

    /** 超时未完成的 running/pending 任务（截止线之前创建） */
    List<RecalculationTask> selectStale(@Param("cutoff") LocalDateTime cutoff);

    int markRunning(@Param("id") Long id);

    int markSucceeded(@Param("id") Long id);

    int markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    int updateProgress(@Param("id") Long id,
                       @Param("successCount") Integer successCount,
                       @Param("failCount") Integer failCount,
                       @Param("failureDetail") String failureDetail);

    int updateRefId(@Param("id") Long id, @Param("refId") Long refId);
}
