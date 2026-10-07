package com.duoduocode.service.recurring.service;

import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recurring.dto.SubscriptionOverviewVO;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * R004 U401–U403：订阅折算 / 涨价判定 / 计划窗口推算（plannedDatesInRange 泛化）。
 * R003 的 plannedDatesInMonth 回归由 RecurringTemplateCalendarTest 既有用例持续覆盖。
 */
class SubscriptionOverviewUnitTest {

    private final RecurringTemplateService service =
            new RecurringTemplateService(mock(RecurringTemplateMapper.class),
                    mock(NotificationService.class));

    private RecurringTemplate tpl(String frequency, String amount) {
        RecurringTemplate t = new RecurringTemplate();
        t.setId(1L);
        t.setUserId(9L);
        t.setName("PyTest订阅模板");
        t.setType("expense");
        t.setAmount(new BigDecimal(amount));
        t.setFrequency(frequency);
        t.setExecutedCount(0);
        t.setStatus("active");
        return t;
    }

    private void assertDecimal(String want, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(want).compareTo(actual) == 0,
                "期望 " + want + " 实际 " + actual);
    }

    @Test
    void U401_四频率折算月额() {
        assertDecimal("30.00", service.convertToMonthly("monthly", new BigDecimal("30")));
        assertDecimal("43.33", service.convertToMonthly("weekly", new BigDecimal("10")));   // 10×52÷12
        assertDecimal("60.83", service.convertToMonthly("daily", new BigDecimal("2")));     // 2×365÷12
        assertDecimal("10.00", service.convertToMonthly("yearly", new BigDecimal("120")));  // 120÷12
    }

    @Test
    void U401_HALF_UP两位舍入() {
        // 25×52÷12 = 108.333… → 108.33；1.25×365÷12 = 38.0208… → 38.02
        assertDecimal("108.33", service.convertToMonthly("weekly", new BigDecimal("25")));
        assertDecimal("38.02", service.convertToMonthly("daily", new BigDecimal("1.25")));
        assertEquals(2, service.convertToMonthly("weekly", new BigDecimal("25")).scale(),
                "折算结果应固定 2 位小数");
    }

    @Test
    void U402_涨价_差额与幅度() {
        SubscriptionOverviewVO.Item item =
                service.buildSubscriptionItem(tpl("monthly", "20"), new BigDecimal("15"));
        assertTrue(item.getPriceIncreased(), "20 > 15 应判定涨价");
        assertDecimal("5.00", item.getIncreaseAmount());
        assertDecimal("33.3", item.getIncreasePercent());  // 5÷15×100，1 位小数
        assertDecimal("20.00", item.getAmount());
        assertDecimal("20.00", item.getMonthlyAmount());
    }

    @Test
    void U402_降价与等额_不标记() {
        SubscriptionOverviewVO.Item down =
                service.buildSubscriptionItem(tpl("monthly", "10"), new BigDecimal("15"));
        assertFalse(down.getPriceIncreased(), "降价不应标记涨价");
        assertNull(down.getIncreaseAmount());
        assertNull(down.getIncreasePercent());
        SubscriptionOverviewVO.Item equal =
                service.buildSubscriptionItem(tpl("monthly", "15"), new BigDecimal("15"));
        assertFalse(equal.getPriceIncreased(), "等额不属于涨价");
    }

    @Test
    void U402_无基准_恒不误报() {
        SubscriptionOverviewVO.Item item = service.buildSubscriptionItem(tpl("monthly", "25"), null);
        assertNull(item.getLastPaidAmount(), "无基准 lastPaidAmount 应为 null");
        assertFalse(item.getPriceIncreased(), "无基准不误报（验收 A-3）");
        assertNull(item.getIncreaseAmount());
        assertNull(item.getIncreasePercent());
    }

    @Test
    void U403_窗口起止含端点() {
        RecurringTemplate t = tpl("monthly", "30");
        t.setStartDate(LocalDate.of(2026, 1, 1));
        t.setDayOfMonth(8);
        assertEquals(List.of(LocalDate.of(2026, 8, 8)),
                service.plannedDatesInRange(t, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));
        // 起止恰好压在触发日上 → 两端均含
        assertEquals(List.of(LocalDate.of(2026, 8, 8)),
                service.plannedDatesInRange(t, LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 8)));
    }

    @Test
    void U403_跨月窗口() {
        RecurringTemplate t = tpl("monthly", "30");
        t.setStartDate(LocalDate.of(2026, 1, 1));
        t.setDayOfMonth(8);
        assertEquals(List.of(LocalDate.of(2026, 7, 8), LocalDate.of(2026, 8, 8)),
                service.plannedDatesInRange(t, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31)));
    }

    @Test
    void U403_endDate与maxCount截断() {
        RecurringTemplate t = tpl("monthly", "30");
        t.setStartDate(LocalDate.of(2026, 1, 1));
        t.setDayOfMonth(8);
        t.setEndDate(LocalDate.of(2026, 7, 20));
        assertEquals(List.of(LocalDate.of(2026, 7, 8)),
                service.plannedDatesInRange(t, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31)),
                "endDate 之后不应出现");

        RecurringTemplate t2 = tpl("monthly", "30");
        t2.setStartDate(LocalDate.of(2026, 7, 1));
        t2.setDayOfMonth(8);
        t2.setMaxCount(1);
        assertEquals(List.of(LocalDate.of(2026, 7, 8)),
                service.plannedDatesInRange(t2, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)),
                "maxCount=1 时 8 月为第 2 期，不应出现");
    }
}
