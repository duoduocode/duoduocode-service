package com.duoduocode.service.billimport;

import java.time.LocalDateTime;

/**
 * 账单导入批次（日志性质，不做软删）
 */
public class ImportBatch {

    private Long id;

    private Long userId;

    /**
     * alipay/wechat
     */
    private String source;

    private String fileName;

    private Integer totalRows;

    private Integer importedCount;

    /**
     * 判重跳过
     */
    private Integer skippedCount;

    /**
     * 不计收支/非法行
     */
    private Integer ignoredCount;

    /**
     * 自动创建的账户名，逗号分隔
     */
    private String createdAccounts;

    /**
     * confirmed/failed
     */
    private String status;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
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

    public String getCreatedAccounts() {
        return createdAccounts;
    }

    public void setCreatedAccounts(String createdAccounts) {
        this.createdAccounts = createdAccounts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
