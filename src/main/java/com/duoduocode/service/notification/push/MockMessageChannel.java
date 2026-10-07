package com.duoduocode.service.notification.push;

import com.duoduocode.service.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 外发通道：仅打日志（app.wechat.subscribe.enabled=false 时的默认实现）
 */
@Slf4j
@Component
public class MockMessageChannel implements MessageChannel {

    @Override
    public void push(Notification notification) {
        log.info("[MOCK推送] userId={} type={} title={}",
                notification.getUserId(), notification.getType(), notification.getTitle());
    }
}
