package com.duoduocode.service.recalculation;

import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recalculation.entity.RecalculationTask;
import com.duoduocode.service.recalculation.mapper.RecalculationTaskMapper;
import com.duoduocode.service.recalculation.service.RecalculationTaskService;
import com.duoduocode.service.recurring.service.RecurringTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * R003 U303/U306：超时任务扫描与任务级异常兜底。
 */
@ExtendWith(MockitoExtension.class)
class RecalculationTaskServiceTest {

    @Mock
    private RecalculationTaskMapper taskMapper;
    @Mock
    private RecurringTemplateService templateService;
    @Mock
    private NotificationService notificationService;

    private RecalculationTaskService buildService() {
        return new RecalculationTaskService(taskMapper, templateService,
                notificationService);
    }

    private RecalculationTask task(Long id, String status) {
        RecalculationTask t = new RecalculationTask();
        t.setId(id);
        t.setUserId(9L);
        t.setTaskType("batch_confirm");
        t.setStatus(status);
        t.setTotalCount(2);
        t.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        return t;
    }

    @Test
    void U303_超时任务被置failed_截止线为30分钟() {
        RecalculationTask stale1 = task(1L, "running");
        RecalculationTask stale2 = task(2L, "pending");
        when(taskMapper.selectStale(any(LocalDateTime.class)))
                .thenReturn(List.of(stale1, stale2));

        int failed = buildService().failStaleTasks(LocalDateTime.now());

        assertEquals(2, failed);
        verify(taskMapper, times(1)).markFailed(eq(1L), contains("中断"));
        verify(taskMapper, times(1)).markFailed(eq(2L), anyString());
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(taskMapper).selectStale(cutoff.capture());
        long minutes = java.time.Duration.between(
                cutoff.getValue(), LocalDateTime.now().minusMinutes(30)).toMinutes();
        assertTrue(Math.abs(minutes) <= 1, "截止线应为约 30 分钟前: " + cutoff.getValue());
    }

    @Test
    void U303_无超时任务_返回0() {
        when(taskMapper.selectStale(any(LocalDateTime.class))).thenReturn(List.of());
        assertEquals(0, buildService().failStaleTasks(LocalDateTime.now()));
        verify(taskMapper, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void U306_执行中RuntimeException_任务置failed带原因() {
        RecalculationTask running = task(5L, "running");
        when(taskMapper.selectById(5L)).thenReturn(running);
        when(templateService.confirmSingleTemplate(anyLong(), any()))
                .thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() ->
                buildService().executeBatchConfirm(5L, List.of(1L, 2L)));

        verify(taskMapper, times(1)).markFailed(eq(5L), contains("boom"));
        verify(taskMapper, never()).markSucceeded(anyLong());
    }

    @Test
    void U306_任务级成功_落progress与通知() {
        RecalculationTask running = task(6L, "running");
        when(taskMapper.selectById(6L)).thenReturn(running);
        when(templateService.confirmSingleTemplate(anyLong(), any()))
                .thenReturn(com.duoduocode.service.recurring.dto.ItemResult.ok(1L));

        buildService().executeBatchConfirm(6L, List.of(1L, 2L));

        verify(taskMapper, times(1)).markSucceeded(6L);
        verify(taskMapper, atLeastOnce()).updateProgress(eq(6L), anyInt(), anyInt(), any());
        verify(notificationService, times(1)).createIfAbsent(any());
    }
}
