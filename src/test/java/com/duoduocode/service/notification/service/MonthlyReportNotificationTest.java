package com.duoduocode.service.notification.service;

import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.report.entity.MonthlyReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R004 U406：月报生成通知构造 buildMonthlyReportNotification。
 */
class MonthlyReportNotificationTest {

    private final NotificationService service =
            NotificationTestSupport.buildService(org.mockito.Mockito.mock(
                    com.duoduocode.service.notification.mapper.NotificationMapper.class));

    private MonthlyReport report(String month, String savingsRate,
                                 String netWorthStart, String netWorthEnd) {
        MonthlyReport r = new MonthlyReport();
        r.setId(5L);
        r.setUserId(9L);
        r.setMonth(month);
        r.setTotalIncome(new BigDecimal("8000.00"));
        r.setTotalExpense(new BigDecimal("5000.00"));
        r.setSavingsRate(savingsRate == null ? null : new BigDecimal(savingsRate));
        r.setNetWorthStart(new BigDecimal(netWorthStart));
        r.setNetWorthEnd(new BigDecimal(netWorthEnd));
        r.setSubscriptionSpend(new BigDecimal("600.00"));
        return r;
    }

    @Test
    void U406_通知要素_含月份动态标题() {
        Notification n = service.buildMonthlyReportNotification(report("2026-07", "37.50", "10000.00", "13000.00"));
        assertEquals(9L, n.getUserId());
        assertEquals("notice", n.getMsgType());
        assertEquals("monthly_report", n.getType());
        assertEquals("report", n.getRefType());
        assertEquals(5L, n.getRefId());
        assertEquals("report:9:2026-07", n.getDedupKey());
        assertTrue(n.getTitle().contains("7月财务月报"), "标题应含动态月份: " + n.getTitle());
    }

    @Test
    void U406_内容摘要_储蓄率与净资产变动() {
        Notification n = service.buildMonthlyReportNotification(report("2026-07", "37.50", "10000.00", "13000.00"));
        assertTrue(n.getContent().contains("37.5"), "内容应含储蓄率: " + n.getContent());
        assertTrue(n.getContent().contains("3000"), "内容应含净资产变动: " + n.getContent());
    }

    @Test
    void U406_月份动态_一月不带前导零() {
        Notification n = service.buildMonthlyReportNotification(report("2026-01", "12.00", "100.00", "150.00"));
        assertTrue(n.getTitle().contains("1月财务月报"), "标题月份应去前导零: " + n.getTitle());
        assertEquals("report:9:2026-01", n.getDedupKey());
    }

    @Test
    void U406_储蓄率null_内容仍可拼装() {
        Notification n = service.buildMonthlyReportNotification(report("2026-07", null, "10000.00", "13000.00"));
        assertNotNull(n.getContent(), "零收入月内容不应为空");
        assertNotNull(n.getTitle());
    }
}
