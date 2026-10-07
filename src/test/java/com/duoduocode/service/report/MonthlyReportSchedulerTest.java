package com.duoduocode.service.report;

import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.report.dto.MonthlyReportDetailVO;
import com.duoduocode.service.report.entity.MonthlyReport;
import com.duoduocode.service.report.mapper.MonthlyReportMapper;
import com.duoduocode.service.report.service.MonthlyReportService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * R004 U411：月报调度器逐用户隔离——单用户异常被捕获、后续用户继续、
 * 异常用户不产生通知（半截快照由逐用户事务边界保证，本测试锁 try/catch 编排）。
 */
class MonthlyReportSchedulerTest {

    private final MonthlyReportMapper monthlyReportMapper = mock(MonthlyReportMapper.class);
    private final MonthlyReportService monthlyReportService = mock(MonthlyReportService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final MonthlyReportScheduler scheduler =
            new MonthlyReportScheduler(monthlyReportMapper, monthlyReportService, notificationService);

    private MonthlyReportDetailVO detail(Long id) {
        MonthlyReportDetailVO vo = new MonthlyReportDetailVO();
        vo.setId(id);
        vo.setMonth(YearMonth.now().minusMonths(1).toString());
        return vo;
    }

    private MonthlyReport entity(Long userId, Long id) {
        MonthlyReport r = new MonthlyReport();
        r.setId(id);
        r.setUserId(userId);
        r.setMonth(YearMonth.now().minusMonths(1).toString());
        return r;
    }

    @Test
    void U411_单用户异常_后续用户继续且不外抛() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        when(monthlyReportMapper.selectUserIdsWithTransactions(lastMonth)).thenReturn(List.of(1L, 2L, 3L));
        when(monthlyReportService.generate(1L, lastMonth)).thenReturn(detail(101L));
        when(monthlyReportService.generate(2L, lastMonth))
                .thenThrow(new RuntimeException("模拟单用户数据异常"));
        when(monthlyReportService.generate(3L, lastMonth)).thenReturn(detail(103L));
        when(monthlyReportMapper.selectByUserIdAndMonth(1L, lastMonth)).thenReturn(entity(1L, 101L));
        when(monthlyReportMapper.selectByUserIdAndMonth(3L, lastMonth)).thenReturn(entity(3L, 103L));
        when(notificationService.buildMonthlyReportNotification(any(MonthlyReport.class)))
                .thenAnswer(inv -> {
                    MonthlyReport r = inv.getArgument(0);
                    Notification n = new Notification();
                    n.setUserId(r.getUserId());
                    n.setMsgType("notice");
                    n.setType("monthly_report");
                    n.setDedupKey("report:" + r.getUserId() + ":" + r.getMonth());
                    return n;
                });
        when(notificationService.insertIfAbsent(any())).thenReturn(true);

        assertDoesNotThrow(scheduler::generateLastMonthReports, "单用户异常不应中断整轮调度");

        verify(monthlyReportService).generate(1L, lastMonth);
        verify(monthlyReportService).generate(2L, lastMonth);
        verify(monthlyReportService).generate(3L, lastMonth);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(2)).insertIfAbsent(captor.capture());
        List<String> dedupKeys = captor.getAllValues().stream()
                .map(Notification::getDedupKey).toList();
        assertEquals(List.of("report:1:" + lastMonth, "report:3:" + lastMonth), dedupKeys,
                "异常用户 2 不应产生通知");
    }

    @Test
    void U411_无待生成用户_空转不报错() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        when(monthlyReportMapper.selectUserIdsWithTransactions(lastMonth)).thenReturn(List.of());

        assertDoesNotThrow(scheduler::generateLastMonthReports);
        verify(monthlyReportService, never()).generate(any(), any());
        verify(notificationService, never()).insertIfAbsent(any());
    }
}
