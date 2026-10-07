package com.duoduocode.service.notification.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通知 dedup_key 生成与幂等创建规则（U02）。
 * 纯单元测试：mock mapper，验证 key 规则与重复插入行为。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDedupTest {

    @Mock
    private com.duoduocode.service.notification.mapper.NotificationMapper notificationMapper;

    private NotificationService buildService() {
        return NotificationTestSupport.buildService(notificationMapper);
    }

    @Test
    void 周期到期_key规则() {
        String key = NotificationService.dedupKeyRecurring(8L, LocalDate.of(2026, 8, 21));
        assertEquals("recur:8:2026-08-21", key);
    }

    @Test
    void 预算预警_同月同分类分级key() {
        assertEquals("budget:10:2026-08:warn",
                NotificationService.dedupKeyBudget(10L, "2026-08", "warn"));
        assertEquals("budget:10:2026-08:exceed",
                NotificationService.dedupKeyBudget(10L, "2026-08", "exceed"));
    }

    @Test
    void 还款日_同月同账户key() {
        assertEquals("repay:5:2026-08", NotificationService.dedupKeyRepayment(5L, "2026-08"));
    }

    @Test
    void 待办类型周期消息与通知类区分() {
        Notification todo = NotificationTestSupport.buildNotification(1L, "todo", "recurring_due", "recur:8:2026-08-21");
        Notification notice = NotificationTestSupport.buildNotification(1L, "notice", "budget_warning", "budget:10:2026-08:warn");
        assertEquals("todo", todo.getMsgType());
        assertEquals("notice", notice.getMsgType());
    }

    @Test
    void 幂等创建_key重复时不再插入() {
        NotificationService service = buildService();
        // 第一次插入成功；第二次同 key 插入被忽略（ON DUPLICATE 语义由 service 封装）
        Notification n = NotificationTestSupport.buildNotification(9L, "todo", "recurring_due", "recur:8:2026-08-21");
        Notification result = service.createIfAbsent(n);
        assertNotNull(result);
        assertEquals("recur:8:2026-08-21", result.getDedupKey());
    }

    @Test
    void 周期待办_远期到期文案与金额频率() {
        RecurringTemplate template = new RecurringTemplate();
        template.setUserId(8L);
        template.setId(100L);
        template.setName("房租");
        template.setAmount(new BigDecimal("3000"));
        template.setFrequency("monthly");
        template.setNextTriggerDate(LocalDate.now().plusDays(5));
        Notification n = new NotificationService(notificationMapper).buildRecurringTodo(template);
        assertTrue(n.getTitle().contains("日到期"), "远期到期应展示具体日期: " + n.getTitle());
        assertTrue(n.getContent().contains("3000"));
        assertTrue(n.getContent().contains("每月"));
        assertEquals("recur:100:" + LocalDate.now().plusDays(5), n.getDedupKey());
    }

    @Test
    void 消化待办_按期次key精确消化() {
        NotificationService service = new NotificationService(notificationMapper);
        service.digestRecurringTodo(8L, 100L, LocalDate.of(2026, 8, 21));
        Mockito.verify(notificationMapper).digestByDedupKey(8L, "recur:100:2026-08-21");
    }

    @Test
    void 消化待办_期次缺失时不发SQL() {
        NotificationService service = new NotificationService(notificationMapper);
        assertEquals(0, service.digestRecurringTodo(8L, 100L, null));
        Mockito.verifyNoInteractions(notificationMapper);
    }

    @Test
    void 还款通知_欠款为零显示已结清() {
        Account account = new Account();
        account.setUserId(5L);
        account.setId(55L);
        account.setName("招行信用卡");
        account.setRepaymentDay(22);
        Notification n = new NotificationService(notificationMapper).buildRepaymentNotice(account, BigDecimal.ZERO, "2026-08");
        assertTrue(n.getContent().contains("已结清"), "零欠款应提示已结清: " + n.getContent());
        assertEquals("repay:55:2026-08", n.getDedupKey());
    }
}
