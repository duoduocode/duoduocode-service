package com.duoduocode.service.notification.service;

import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.mapper.NotificationMapper;
import com.duoduocode.service.recalculation.entity.RecalculationTask;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R003 U304：批量任务完成通知构造 buildTaskDoneNotification。
 */
class TaskDoneNotificationTest {

    private final NotificationMapper mapper = Mockito.mock(NotificationMapper.class);
    private final NotificationService service = NotificationTestSupport.buildService(mapper);

    private RecalculationTask task(String taskType, int success, int fail) {
        RecalculationTask t = new RecalculationTask();
        t.setId(5L);
        t.setUserId(9L);
        t.setTaskType(taskType);
        t.setStatus("succeeded");
        t.setTotalCount(success + fail);
        t.setSuccessCount(success);
        t.setFailCount(fail);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void U304_批量确认完成_通知要素() {
        Notification n = service.buildTaskDoneNotification(task("batch_confirm", 10, 0));
        assertEquals(9L, n.getUserId());
        assertEquals("notice", n.getMsgType());
        assertEquals("task_done", n.getType());
        assertEquals("task:5", n.getDedupKey());
        assertEquals("task", n.getRefType());
        assertEquals(5L, n.getRefId());
        assertTrue(n.getTitle().contains("账目已更新"), "标题: " + n.getTitle());
        assertTrue(n.getContent().contains("已确认"), "内容: " + n.getContent());
        assertTrue(n.getContent().contains("10"), "内容应含计数: " + n.getContent());
    }

    @Test
    void U304_导入完成_文案区分() {
        Notification n = service.buildTaskDoneNotification(task("import", 50, 0));
        assertEquals("task_done", n.getType());
        assertTrue(n.getContent().contains("已导入"), "内容: " + n.getContent());
    }

    @Test
    void U304_带失败明细_内容含失败摘要() {
        Notification n = service.buildTaskDoneNotification(task("batch_confirm", 3, 2));
        assertTrue(n.getContent().contains("2"), "内容应含失败计数: " + n.getContent());
    }
}
