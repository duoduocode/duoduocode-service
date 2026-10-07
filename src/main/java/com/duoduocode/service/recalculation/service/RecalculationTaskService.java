package com.duoduocode.service.recalculation.service;

import com.duoduocode.service.billimport.ImportConfirmDTO;
import com.duoduocode.service.billimport.ImportConfirmVO;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recalculation.dto.RecalculationTaskVO;
import com.duoduocode.service.recalculation.entity.RecalculationTask;
import com.duoduocode.service.recalculation.mapper.RecalculationTaskMapper;
import com.duoduocode.service.recurring.dto.BatchConfirmVO;
import com.duoduocode.service.recurring.dto.ItemResult;
import com.duoduocode.service.recurring.service.RecurringTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量誊录异步任务服务：创建/执行/查询/超时兜底
 */
@Slf4j
@Service
public class RecalculationTaskService {

    private static final int STALE_MINUTES = 30;

    private final RecalculationTaskMapper taskMapper;
    private final RecurringTemplateService templateService;
    private final NotificationService notificationService;

    public RecalculationTaskService(RecalculationTaskMapper taskMapper,
                                    RecurringTemplateService templateService,
                                    NotificationService notificationService) {
        this.taskMapper = taskMapper;
        this.templateService = templateService;
        this.notificationService = notificationService;
    }

    /**
     * 创建任务（立即 running）
     */
    public Long createTask(Long userId, String taskType, Integer totalCount) {
        RecalculationTask task = new RecalculationTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus("running");
        task.setTotalCount(totalCount == null ? 0 : totalCount);
        taskMapper.insert(task);
        return task.getId();
    }

    /**
     * 查询任务详情（不存在/越权 → 业务异常）
     */
    public RecalculationTaskVO getTaskDetail(Long userId, Long taskId) {
        RecalculationTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        return convertToVO(task);
    }

    /**
     * 异步执行批量确认：逐笔 confirmSingleTemplate，单笔失败只记明细不中断
     */
    @Async("recalcExecutor")
    public void executeBatchConfirm(Long taskId, List<Long> templateIds) {
        RecalculationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务 {} 不存在，跳过执行", taskId);
            return;
        }
        try {
            List<ItemResult> results = new ArrayList<>();
            for (Long templateId : templateIds) {
                results.add(templateService.confirmSingleTemplate(templateId, task.getUserId()));
            }
            completeTask(task, results, null);
        } catch (RuntimeException e) {
            log.error("批量确认任务 {} 执行失败", taskId, e);
            taskMapper.markFailed(taskId, "任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行账单导入
     */
    @Async("recalcExecutor")
    public void executeImport(Long taskId, Long userId, ImportConfirmDTO dto, ImportBody body) {
        RecalculationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务 {} 不存在，跳过执行", taskId);
            return;
        }
        try {
            ImportConfirmVO vo = body.run();
            int success = vo.getImportedCount() == null ? 0 : vo.getImportedCount();
            int fail = vo.getSkippedCount() == null ? 0 : vo.getSkippedCount();
            task.setSuccessCount(success);
            task.setFailCount(fail);
            taskMapper.updateProgress(taskId, success, fail, null);
            if (vo.getBatchId() != null) {
                taskMapper.updateRefId(taskId, vo.getBatchId());
            }
            taskMapper.markSucceeded(taskId);
            notificationService.createIfAbsent(notificationService.buildTaskDoneNotification(task));
        } catch (RuntimeException e) {
            log.error("导入任务 {} 执行失败", taskId, e);
            taskMapper.markFailed(taskId, "导入任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 超时兜底：超 30 分钟未完成的 running/pending 置 failed（含服务重启残留）
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void sweepStaleTasks() {
        int failed = failStaleTasks(LocalDateTime.now());
        if (failed > 0) {
            log.warn("超时任务扫描：{} 个任务置 failed", failed);
        }
    }

    public int failStaleTasks(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(STALE_MINUTES);
        List<RecalculationTask> stale = taskMapper.selectStale(cutoff);
        for (RecalculationTask task : stale) {
            taskMapper.markFailed(task.getId(), "处理超时或服务重启中断，请重试");
        }
        return stale.size();
    }

    private void completeTask(RecalculationTask task, List<ItemResult> results, Long refId) {
        BatchConfirmVO summary = BatchConfirmVO.summarize(results);
        task.setSuccessCount(summary.getSuccessCount());
        task.setFailCount(summary.getFailCount());
        String failureDetail = failureDetailJson(summary);
        taskMapper.updateProgress(task.getId(), summary.getSuccessCount(),
                summary.getFailCount(), failureDetail);
        if (refId != null) {
            taskMapper.updateRefId(task.getId(), refId);
        }
        taskMapper.markSucceeded(task.getId());
        notificationService.createIfAbsent(notificationService.buildTaskDoneNotification(task));
    }

    private String failureDetailJson(BatchConfirmVO summary) {
        if (summary.getFailures().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (BatchConfirmVO.Failure f : summary.getFailures()) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append("{\"id\":").append(f.getId())
                    .append(",\"reason\":\"")
                    .append(f.getReason() == null ? "" : f.getReason().replace("\"", "'"))
                    .append("\"}");
        }
        return sb.append("]").toString();
    }

    private RecalculationTaskVO convertToVO(RecalculationTask task) {
        RecalculationTaskVO vo = new RecalculationTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(task.getTotalCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setFailureDetail(task.getFailureDetail());
        vo.setRefId(task.getRefId());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        return vo;
    }

    /**
     * 导入执行体：由 ImportService 以代理闭包传入，保证事务语义
     */
    @FunctionalInterface
    public interface ImportBody {

        ImportConfirmVO run();
    }
}
