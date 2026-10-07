package com.duoduocode.service.recalculation.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量誊录异步任务
 */
@Data
public class RecalculationTask {

    private Long id;

    private Long userId;

    /** batch_confirm/import */
    private String taskType;

    /** pending/running/succeeded/failed */
    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    /** 失败明细 JSON [{id,reason}] */
    private String failureDetail;

    /** 任务级异常 */
    private String errorMsg;

    /** 关联对象（import=import_batch_id） */
    private Long refId;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
