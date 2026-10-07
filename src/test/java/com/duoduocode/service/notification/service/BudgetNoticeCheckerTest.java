package com.duoduocode.service.notification.service;

import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
import com.duoduocode.service.budget.service.BudgetNoticeChecker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 预算通知阈值判定（U03）：79.9/80/99.9/100/116 边界。
 */
class BudgetNoticeCheckerTest {

    private final BudgetNoticeChecker checker = new BudgetNoticeChecker();

    @Test
    void 阈值_低于80无通知() {
        assertEquals(BudgetNoticeChecker.Level.NONE, checker.level(new BigDecimal("79.9")));
        assertEquals(BudgetNoticeChecker.Level.NONE, checker.level(new BigDecimal("0")));
        assertEquals(BudgetNoticeChecker.Level.NONE, checker.level(new BigDecimal("79.999")));
    }

    @Test
    void 阈值_80整预警() {
        assertEquals(BudgetNoticeChecker.Level.WARN, checker.level(new BigDecimal("80.0")));
        assertEquals(BudgetNoticeChecker.Level.WARN, checker.level(new BigDecimal("80.00")));
        assertEquals(BudgetNoticeChecker.Level.WARN, checker.level(new BigDecimal("99.9")));
    }

    @Test
    void 阈值_100整超限() {
        assertEquals(BudgetNoticeChecker.Level.EXCEED, checker.level(new BigDecimal("100.0")));
        assertEquals(BudgetNoticeChecker.Level.EXCEED, checker.level(new BigDecimal("116.5")));
        assertEquals(BudgetNoticeChecker.Level.EXCEED, checker.level(new BigDecimal("300")));
    }

    @Test
    void 阈值_null按无通知处理() {
        assertEquals(BudgetNoticeChecker.Level.NONE, checker.level(null));
    }

    @Test
    void 使用率计算_含结转口径() {
        // spent=860, budget=1000, carryover=0 → 86%
        assertEquals(new BigDecimal("86.00"),
                checker.usagePercent(new BigDecimal("860"), new BigDecimal("1000"), BigDecimal.ZERO));
        // 含结转：spent=900, budget=1000, carryover=200 → (900)/(1000+200)=75%
        assertEquals(new BigDecimal("75.00"),
                checker.usagePercent(new BigDecimal("900"), new BigDecimal("1000"), new BigDecimal("200")));
        // 预算为 0 且有支出 → 100+（视为超限）
        assertEquals(BudgetNoticeChecker.Level.EXCEED,
                checker.level(checker.usagePercent(new BigDecimal("1"), BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    @Test
    void 通知文案_含量与百分比() {
        String text = checker.buildContent("餐饮", new BigDecimal("86.00"),
                new BigDecimal("860"), new BigDecimal("1000"));
        assertTrue(text.contains("86"));
        assertTrue(text.contains("餐饮"));
    }

    @Test
    void 通知文案_null百分比显示横杠() {
        String text = checker.buildContent("餐饮", null, BigDecimal.TEN, BigDecimal.TEN);
        assertTrue(text.contains("-"), "null 百分比应以横杠占位: " + text);
    }

    @Test
    void 使用率_null入参按零处理() {
        assertEquals(new BigDecimal("0.00"), checker.usagePercent(null, null, null));
        assertEquals(new BigDecimal("100.00"),
                checker.usagePercent(BigDecimal.ONE, null, null));
    }

    @Test
    void 事务钩子_空参直接返回() {
        BudgetNoticeChecker bare = new BudgetNoticeChecker();
        assertDoesNotThrow(() -> bare.checkAfterTransaction(null, List.of(1L)));
        assertDoesNotThrow(() -> bare.checkAfterTransaction(1L, null));
        assertDoesNotThrow(() -> bare.checkAfterTransaction(1L, List.of()));
    }

    @Test
    void 事务钩子_无事务上下文同步检查无预算分类() {
        BudgetNoticeChecker bare = new BudgetNoticeChecker();
        // 无事务上下文（单测默认）→ 直接同步检查；dailyBudgetMapper 为 null → checkOne 安全返回
        assertDoesNotThrow(() -> bare.checkAfterTransaction(1L, List.of(10L)));
    }

    @Test
    void 单分类检查_无预算记录返回零() {
        BudgetNoticeChecker s = new BudgetNoticeChecker();
        DailyBudgetMapper dbMapper = Mockito.mock(DailyBudgetMapper.class);
        Mockito.when(dbMapper.selectByUserIdAndCategoryIdAndMonth(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString())).thenReturn(null);
        ReflectionTestUtils.setField(s, "dailyBudgetMapper", dbMapper);
        ReflectionTestUtils.setField(s, "notificationService",
                Mockito.mock(NotificationService.class));
        assertDoesNotThrow(() -> s.checkAfterTransaction(1L, List.of(10L)));
    }

    @Test
    void 兜底扫描_mapper缺失返回零() {
        BudgetNoticeChecker bare = new BudgetNoticeChecker();
        assertEquals(0, bare.scanMonthFallback());
    }

    private static DailyBudget budget(Long categoryId, BigDecimal monthlyBudget) {
        DailyBudget b = new DailyBudget();
        b.setUserId(1L);
        b.setCategoryId(categoryId);
        b.setMonthlyBudget(monthlyBudget);
        return b;
    }

    @Test
    void 兜底扫描_零预算与空预算跳过() {
        BudgetNoticeChecker s = new BudgetNoticeChecker();
        DailyBudgetMapper dbMapper = Mockito.mock(DailyBudgetMapper.class);
        Mockito.when(dbMapper.selectByMonth(Mockito.anyString()))
                .thenReturn(List.of(budget(10L, null), budget(11L, BigDecimal.ZERO)));
        ReflectionTestUtils.setField(s, "dailyBudgetMapper", dbMapper);
        ReflectionTestUtils.setField(s, "notificationService",
                Mockito.mock(NotificationService.class));
        assertEquals(0, s.scanMonthFallback());
        Mockito.verify(dbMapper, Mockito.never())
                .selectByUserIdAndCategoryIdAndMonth(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    void 兜底扫描_查询异常逐分类容错() {
        BudgetNoticeChecker s = new BudgetNoticeChecker();
        DailyBudgetMapper dbMapper = Mockito.mock(DailyBudgetMapper.class);
        Mockito.when(dbMapper.selectByMonth(Mockito.anyString()))
                .thenReturn(List.of(budget(10L, new BigDecimal("500"))));
        Mockito.when(dbMapper.selectByUserIdAndCategoryIdAndMonth(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString()))
                .thenThrow(new RuntimeException("模拟查询失败"));
        ReflectionTestUtils.setField(s, "dailyBudgetMapper", dbMapper);
        ReflectionTestUtils.setField(s, "notificationService",
                Mockito.mock(NotificationService.class));
        assertEquals(0, s.scanMonthFallback());
    }
}
