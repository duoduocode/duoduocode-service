package com.duoduocode.service.notification.push;

import com.duoduocode.service.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息外发器：外发失败仅记日志，不影响站内消息主流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final MessageChannel messageChannel;

    public void publish(Notification notification) {
        try {
            messageChannel.push(notification);
        } catch (Exception e) {
            log.warn("消息外发失败（不影响站内消息）: type={}, title={}",
                    notification.getType(), notification.getTitle(), e);
        }
    }
}
