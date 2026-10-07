package com.duoduocode.service.billimport;

import java.util.List;

/**
 * 导入确认结果 VO
 */
public class ImportConfirmVO {

    /** 异步受理任务ID（≥50 笔时返回，同步路径为 null） */
    private Long taskId;

    private Long batchId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    private Integer importedCount;

    private Integer skippedCount;

    private Integer ignoredCount;

    private List<String> createdAccounts;

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(Integer importedCount) {
        this.importedCount = importedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Integer getIgnoredCount() {
        return ignoredCount;
    }

    public void setIgnoredCount(Integer ignoredCount) {
        this.ignoredCount = ignoredCount;
    }

    public List<String> getCreatedAccounts() {
        return createdAccounts;
    }

    public void setCreatedAccounts(List<String> createdAccounts) {
        this.createdAccounts = createdAccounts;
    }
}
