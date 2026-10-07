package com.duoduocode.service.recurring.service;

import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * R003 U301/U302：月度日历推算 plannedDatesInMonth。
 */
class RecurringTemplateCalendarTest {

    private final RecurringTemplateService service =
            new RecurringTemplateService(mock(RecurringTemplateMapper.class),
                    mock(NotificationService.class));

    private RecurringTemplate tpl(String frequency, LocalDate startDate,
                                  Integer dayOfWeek, Integer dayOfMonth) {
        RecurringTemplate t = new RecurringTemplate();
        t.setId(1L);
        t.setUserId(9L);
        t.setName("PyTest日历模板");
        t.setType("expense");
        t.setAmount(new BigDecimal("88.80"));
        t.setFrequency(frequency);
        t.setStartDate(startDate);
        t.setDayOfWeek(dayOfWeek);
        t.setDayOfMonth(dayOfMonth);
        t.setExecutedCount(0);
        t.setStatus("active");
        return t;
    }

    @Test
    void U301_月度模板_当月恰一期() {
        RecurringTemplate t = tpl("monthly", LocalDate.of(2026, 8, 1), null, 8);
        List<LocalDate> dates = service.plannedDatesInMonth(t, YearMonth.of(2026, 8));
        assertEquals(List.of(LocalDate.of(2026, 8, 8)), dates);
    }

    @Test
    void U301_周度模板_当月全部周一() {
        RecurringTemplate t = tpl("weekly", LocalDate.of(2026, 7, 27), 1, null);
        List<LocalDate> dates = service.plannedDatesInMonth(t, YearMonth.of(2026, 8));
        assertEquals(List.of(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 31)), dates);
    }

    @Test
    void U301_每日模板_当月每天() {
        RecurringTemplate t = tpl("daily", LocalDate.of(2026, 8, 1), null, null);
        assertEquals(31, service.plannedDatesInMonth(t, YearMonth.of(2026, 8)).size());
    }

    @Test
    void U301_开始日期晚于当月_不出计划() {
        RecurringTemplate t = tpl("monthly", LocalDate.of(2026, 9, 1), null, 8);
        assertTrue(service.plannedDatesInMonth(t, YearMonth.of(2026, 8)).isEmpty());
    }

    @Test
    void U302_短月31号_截断到月末() {
        RecurringTemplate t = tpl("monthly", LocalDate.of(2026, 1, 1), null, 31);
        List<LocalDate> feb = service.plannedDatesInMonth(t, YearMonth.of(2026, 2));
        List<LocalDate> jan = service.plannedDatesInMonth(t, YearMonth.of(2026, 1));
        assertEquals(List.of(LocalDate.of(2026, 2, 28)), feb);
        assertEquals(List.of(LocalDate.of(2026, 1, 31)), jan);
    }

    @Test
    void U302_结束日期截断() {
        RecurringTemplate t = tpl("monthly", LocalDate.of(2026, 8, 1), null, 8);
        t.setEndDate(LocalDate.of(2026, 8, 4));
        assertTrue(service.plannedDatesInMonth(t, YearMonth.of(2026, 8)).isEmpty());
    }

    @Test
    void U302_maxCount截断_第二期不出现() {
        RecurringTemplate t = tpl("monthly", LocalDate.of(2026, 7, 1), null, 8);
        t.setMaxCount(1);
        assertTrue(service.plannedDatesInMonth(t, YearMonth.of(2026, 8)).isEmpty(),
                "maxCount=1 时 8 月为第 2 期，不应出现");
        assertEquals(1, service.plannedDatesInMonth(t, YearMonth.of(2026, 7)).size());
    }
}
