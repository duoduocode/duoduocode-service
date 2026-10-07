package com.duoduocode.service.notification.push;

import com.duoduocode.service.notification.entity.Notification;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 消息外发器（U02 补充）：外发失败仅记日志，不影响站内消息主流程。
 */
class NotificationPublisherTest {

    @Test
    void 外发异常被吞掉不影响主流程() {
        MessageChannel channel = Mockito.mock(MessageChannel.class);
        Mockito.doThrow(new RuntimeException("通道不可用")).when(channel).push(Mockito.any());
        NotificationPublisher publisher = new NotificationPublisher(channel);

        Notification n = new Notification();
        n.setType("recurring_due");
        n.setTitle("「房租」今日到期");

        assertDoesNotThrow(() -> publisher.publish(n));
        Mockito.verify(channel).push(n);
    }
}
