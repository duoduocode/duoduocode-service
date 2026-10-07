package com.duoduocode.service.notification.push;

import com.duoduocode.service.notification.entity.Notification;

/**
 * 消息外发通道（本期仅 Mock 打日志；微信订阅消息模板 ID 到位后新增实现）
 */
public interface MessageChannel {

    void push(Notification notification);
}
