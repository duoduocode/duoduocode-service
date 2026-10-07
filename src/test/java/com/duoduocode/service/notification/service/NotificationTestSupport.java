package com.duoduocode.service.notification.service;

import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.mapper.NotificationMapper;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 通知模块测试支撑：构造服务与实体（供 U02 等单元测试复用）。 */
public final class NotificationTestSupport {

    private NotificationTestSupport() {
    }

    public static NotificationService buildService(NotificationMapper mapper) {
        NotificationService service = new NotificationService(mapper);
        Mockito.when(mapper.insertIgnore(Mockito.any(Notification.class))).thenReturn(1);
        return service;
    }

    public static Notification buildNotification(Long userId, String msgType, String type, String dedupKey) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMsgType(msgType);
        n.setType(type);
        n.setTitle("标题-" + type);
        n.setContent("内容");
        n.setDedupKey(dedupKey);
        n.setStatus("unread");
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }

    public static LocalDate date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }
}
