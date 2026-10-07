package com.duoduocode.service.recalculation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量誊录异步任务视图
 */
@Data
public class RecalculationTaskVO {

    private Long id;

    private String taskType;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String failureDetail;

    private Long refId;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
