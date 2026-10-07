package com.duoduocode.service.notification.service;

import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.mapper.NotificationMapper;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 调度器日期算法（U04）：还款日 effectiveDay 短月收敛、提前天数、清理边界。
 */
class NotificationSchedulerTest {

    private final NotificationScheduler scheduler = new NotificationScheduler();

    @Test
    void 短月收敛_31日在2月() {
        // 2026-02 共 28 天：还款日 31 → 28；提醒日 = 28 - 2 = 26
        assertEquals(LocalDate.of(2026, 2, 26),
                scheduler.repaymentNotifyDate(31, LocalDate.of(2026, 2, 10), 2));
    }

    @Test
    void 短月收敛_31日在4月() {
        // 2026-04 共 30 天：还款日 31 → 30；提醒日 = 28
        assertEquals(LocalDate.of(2026, 4, 28),
                scheduler.repaymentNotifyDate(31, LocalDate.of(2026, 4, 10), 2));
    }

    @Test
    void 闰年2月29天() {
        // 2028-02 闰年 29 天：还款日 31 → 29；提醒日 = 27
        assertEquals(LocalDate.of(2028, 2, 27),
                scheduler.repaymentNotifyDate(31, LocalDate.of(2028, 2, 10), 2));
    }

    @Test
    void 常规月不收敛() {
        assertEquals(LocalDate.of(2026, 8, 20),
                scheduler.repaymentNotifyDate(22, LocalDate.of(2026, 8, 15), 2));
    }

    @Test
    void 提前天数配置生效() {
        // advanceDays=5：22 日还款 → 17 日提醒
        assertEquals(LocalDate.of(2026, 8, 17),
                scheduler.repaymentNotifyDate(22, LocalDate.of(2026, 8, 15), 5));
    }

    @Test
    void 提前天数0_当日提醒() {
        assertEquals(LocalDate.of(2026, 8, 22),
                scheduler.repaymentNotifyDate(22, LocalDate.of(2026, 8, 20), 0));
    }

    @Test
    void 今天是否应提醒() {
        assertTrue(scheduler.shouldNotifyToday(22, LocalDate.of(2026, 8, 20), 2));
        assertFalse(scheduler.shouldNotifyToday(22, LocalDate.of(2026, 8, 19), 2));
        assertFalse(scheduler.shouldNotifyToday(22, LocalDate.of(2026, 8, 21), 2));
    }

    @Test
    void 无还款日不提醒() {
        assertFalse(scheduler.shouldNotifyToday(null, LocalDate.of(2026, 8, 20), 2));
        assertFalse(scheduler.shouldNotifyToday(0, LocalDate.of(2026, 8, 20), 2));
    }

    @Test
    void 清理边界_通知90天待办30天() {
        LocalDate now = LocalDate.of(2026, 8, 20);
        // 通知：89 天前可读可删边界内保留提示，91 天前过期
        assertFalse(scheduler.isNoticeExpired(now.minusDays(89), now));
        assertTrue(scheduler.isNoticeExpired(now.minusDays(91), now));
        // 待办 30 天
        assertFalse(scheduler.isTodoExpired(now.minusDays(29), now));
        assertTrue(scheduler.isTodoExpired(now.minusDays(31), now));
    }

    @Test
    void scanDaily_依赖缺失时安全返回() {
        NotificationScheduler bare = new NotificationScheduler();
        assertDoesNotThrow(bare::scanDaily);
        assertDoesNotThrow(bare::cleanup);
    }

    @Test
    void cleanup_删除过期通知与待办() {
        NotificationScheduler s = new NotificationScheduler();
        NotificationMapper mapper = Mockito.mock(NotificationMapper.class);
        Mockito.when(mapper.deleteExpiredReadNotices(Mockito.any())).thenReturn(3);
        Mockito.when(mapper.deleteExpiredDigestedTodos(Mockito.any())).thenReturn(2);
        ReflectionTestUtils.setField(s, "notificationMapper", mapper);
        assertDoesNotThrow(s::cleanup);
        Mockito.verify(mapper).deleteExpiredReadNotices(Mockito.any());
        Mockito.verify(mapper).deleteExpiredDigestedTodos(Mockito.any());
    }

    private static RecurringTemplate template(Long id) {
        RecurringTemplate t = new RecurringTemplate();
        t.setId(id);
        t.setUserId(8L);
        t.setName("模板" + id);
        t.setAmount(BigDecimal.TEN);
        t.setFrequency("daily");
        t.setNextTriggerDate(LocalDate.now());
        return t;
    }

    @Test
    void scanOnce_到期模板生成与单条失败容错() {
        NotificationScheduler s = new NotificationScheduler();
        RecurringTemplateMapper templateMapper = Mockito.mock(RecurringTemplateMapper.class);
        NotificationService notificationService = Mockito.mock(NotificationService.class);
        RecurringTemplate t1 = template(1L);
        RecurringTemplate t2 = template(2L);
        Mockito.when(templateMapper.selectDueTemplatesAll(Mockito.any())).thenReturn(List.of(t1, t2));
        Mockito.when(notificationService.buildRecurringTodo(Mockito.any()))
                .thenReturn(NotificationTestSupport.buildNotification(8L, "todo", "recurring_due", "k"));
        Mockito.when(notificationService.insertIfAbsent(Mockito.any()))
                .thenReturn(true)
                .thenThrow(new RuntimeException("模拟插入失败"));

        ReflectionTestUtils.setField(s, "recurringTemplateMapper", templateMapper);
        ReflectionTestUtils.setField(s, "notificationService", notificationService);
        Map<String, Integer> stats = s.scanOnce();
        assertEquals(1, stats.get("recurring"), "第一条成功、第二条失败应只计 1");
    }
}
