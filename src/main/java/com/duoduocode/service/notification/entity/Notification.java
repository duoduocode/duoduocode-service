package com.duoduocode.service.notification.entity;

import java.time.LocalDateTime;

/**
 * 站内消息/待办实体类
 * 对应 notification 表
 */
public class Notification {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 消息形态：todo-待办, notice-通知
     */
    private String msgType;

    /**
     * 业务类型：recurring_due/budget_warning/budget_exceeded/repayment_due
     */
    private String type;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String content;

    /**
     * 关联对象：template/category/account
     */
    private String refType;

    /**
     * 关联对象ID
     */
    private Long refId;

    /**
     * 幂等键（user_id + dedup_key 唯一）
     */
    private String dedupKey;

    /**
     * 状态：unread/read/digested
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 消化时间（待办完成后）
     */
    private LocalDateTime digestedAt;

    // Getters and Setters

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

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
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

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getDigestedAt() {
        return digestedAt;
    }

    public void setDigestedAt(LocalDateTime digestedAt) {
        this.digestedAt = digestedAt;
    }
}
