package com.duoduocode.service.report.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.budget.dto.DailyBudgetVO;
import com.duoduocode.service.budget.service.BudgetService;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.report.dto.CategoryData;
import com.duoduocode.service.report.dto.MonthlyData;
import com.duoduocode.service.report.dto.MonthlyReportDetailVO;
import com.duoduocode.service.report.entity.MonthlyReport;
import com.duoduocode.service.report.mapper.MonthlyReportMapper;
import com.duoduocode.service.report.mapper.ReportMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * R004 U404/U405/U407/U408：月报快照组装与 detail JSON 往返、生成幂等、
 * 储蓄率计算、预算执行率聚合（数字口径与 I411/I413/I419/I421 对齐）。
 */
class MonthlyReportServiceUnitTest {

    private final MonthlyReportMapper monthlyReportMapper = mock(MonthlyReportMapper.class);
    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final BudgetService budgetService = mock(BudgetService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final MonthlyReportService service =
            new MonthlyReportService(monthlyReportMapper, reportMapper, budgetService, notificationService);

    private void assertDecimal(String want, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(want).compareTo(actual) == 0,
                "期望 " + want + " 实际 " + actual);
    }

    private Account account(Long id, String name, String type, String balance) {
        Account a = new Account();
        a.setId(id);
        a.setUserId(9L);
        a.setName(name);
        a.setType(type);
        a.setIncludeInNetWorth(true);
        a.setCurrentBalance(new BigDecimal(balance));
        return a;
    }

    private DailyBudgetVO budgetVo(Long categoryId, String name, String budget, String used, String usage) {
        DailyBudgetVO vo = new DailyBudgetVO();
        vo.setCategoryId(categoryId);
        vo.setCategoryName(name);
        vo.setMonthlyBudget(new BigDecimal(budget));
        vo.setUsedAmount(new BigDecimal(used));
        vo.setUsagePercent(usage == null ? null : new BigDecimal(usage));
        return vo;
    }

    private CategoryData categoryData(Long categoryId, String name, String amount) {
        CategoryData c = new CategoryData();
        c.setCategoryId(categoryId);
        c.setCategoryName(name);
        c.setAmount(new BigDecimal(amount));
        c.setTransactionCount(1);
        return c;
    }

    /** 造 2026-07 完整数据：收 8000 支 5000；期初净 8000 期末净 10600；预算 96%；订阅 600。 */
    private void stubFullMonth() {
        MonthlyData md = new MonthlyData();
        md.setMonth("2026-07");
        md.setIncome(new BigDecimal("8000.00"));
        md.setExpense(new BigDecimal("5000.00"));
        when(reportMapper.selectMonthlyIncomeExpense(eq(9L), anyString(), anyString()))
                .thenReturn(List.of(md));
        when(monthlyReportMapper.selectByUserIdAndMonth(9L, "2026-07")).thenReturn(null);
        when(monthlyReportMapper.selectAccountBalancesAsOf(eq(9L), eq("2026-07-01")))
                .thenReturn(List.of(account(1L, "招行", "asset", "10000.00"),
                        account(2L, "信用卡", "liability", "-2000.00")));
        when(monthlyReportMapper.selectAccountBalancesAsOf(eq(9L), eq("2026-07-31")))
                .thenReturn(List.of(account(1L, "招行", "asset", "13000.00"),
                        account(2L, "信用卡", "liability", "-2400.00")));
        when(reportMapper.selectCategoryStats(eq(9L), eq("income"), anyString(), anyString()))
                .thenReturn(List.of(categoryData(11L, "工资", "8000.00")));
        when(reportMapper.selectCategoryStats(eq(9L), eq("expense"), anyString(), anyString()))
                .thenReturn(List.of(categoryData(12L, "住房", "2000.00")));
        when(monthlyReportMapper.selectSubscriptionSpend(eq(9L), anyString(), anyString()))
                .thenReturn(new BigDecimal("600.00"));
        when(budgetService.getDailyBudgetUsage(9L, "2026-07"))
                .thenReturn(List.of(budgetVo(12L, "餐饮", "2000.00", "1800.00", "90.00"),
                        budgetVo(13L, "交通", "500.00", "600.00", "120.00")));
        when(monthlyReportMapper.insert(any(MonthlyReport.class))).thenAnswer(inv -> {
            MonthlyReport r = inv.getArgument(0);
            r.setId(5L);
            return 1;
        });
        when(notificationService.insertIfAbsent(any())).thenReturn(true);
    }

    @Test
    void U404_快照组装_核心列与归因() {
        stubFullMonth();
        MonthlyReportDetailVO d = service.generate(9L, "2026-07");

        assertEquals(5L, d.getId());
        assertEquals("2026-07", d.getMonth());
        assertDecimal("8000.00", d.getTotalIncome());
        assertDecimal("5000.00", d.getTotalExpense());
        assertDecimal("3000.00", d.getBalance());
        assertDecimal("37.50", d.getSavingsRate());
        assertDecimal("8000.00", d.getNetWorthStart());   // 10000 − 2000
        assertDecimal("10600.00", d.getNetWorthEnd());    // 13000 − 2400
        assertDecimal("2600.00", d.getNetWorthChange());
        assertDecimal("600.00", d.getSubscriptionSpend());

        assertNotNull(d.getTopAccountChanges(), "归因不应为空");
        assertEquals(2, d.getTopAccountChanges().size());
        assertEquals("招行", d.getTopAccountChanges().get(0).getName());
        assertDecimal("3000.00", d.getTopAccountChanges().get(0).getChange());
        assertEquals("信用卡", d.getTopAccountChanges().get(1).getName());
        assertDecimal("-400.00", d.getTopAccountChanges().get(1).getChange());

        assertEquals("工资", d.getIncomeTop().getName());
        assertDecimal("8000.00", d.getIncomeTop().getAmount());
        assertEquals("住房", d.getExpenseTop().getName());
        assertDecimal("2000.00", d.getExpenseTop().getAmount());

        assertNotNull(d.getBudget());
        assertDecimal("96.00", d.getBudget().getRate());   // (1800+600) ÷ (2000+500)
    }

    @Test
    void U404_detail_JSON序列化反序列化往返一致() {
        stubFullMonth();
        service.generate(9L, "2026-07");

        ArgumentCaptor<MonthlyReport> captor = ArgumentCaptor.forClass(MonthlyReport.class);
        verify(monthlyReportMapper).insert(captor.capture());
        MonthlyReport saved = captor.getValue();
        assertEquals(9L, saved.getUserId());
        assertEquals("2026-07", saved.getMonth());
        assertDecimal("8000.00", saved.getTotalIncome());
        assertDecimal("37.50", saved.getSavingsRate());
        assertDecimal("8000.00", saved.getNetWorthStart());
        assertDecimal("10600.00", saved.getNetWorthEnd());
        assertDecimal("600.00", saved.getSubscriptionSpend());
        assertNotNull(saved.getDetail(), "detail JSON 不应为空");
        assertTrue(saved.getDetail().contains("topAccountChanges"), "detail 应含归因: " + saved.getDetail());
        assertTrue(saved.getDetail().contains("budget"), "detail 应含预算: " + saved.getDetail());

        // 反序列化：落库行经 getDetail 还原为 VO，往返后字段一致
        when(monthlyReportMapper.selectById(5L)).thenReturn(saved);
        MonthlyReportDetailVO re = service.getDetail(5L);
        assertDecimal("8000.00", re.getTotalIncome());
        assertDecimal("37.50", re.getSavingsRate());
        assertDecimal("2600.00", re.getNetWorthChange());
        assertEquals(2, re.getTopAccountChanges().size());
        assertEquals("招行", re.getTopAccountChanges().get(0).getName());
        assertDecimal("3000.00", re.getTopAccountChanges().get(0).getChange());
        assertDecimal("-400.00", re.getTopAccountChanges().get(1).getChange());
        assertEquals("工资", re.getIncomeTop().getName());
        assertDecimal("8000.00", re.getIncomeTop().getAmount());
        assertDecimal("96.00", re.getBudget().getRate());
        assertEquals(2, re.getBudget().getItems().size());
    }

    @Test
    void U405_幂等_已存在回读不重算不重发() {
        MonthlyReport existing = new MonthlyReport();
        existing.setId(7L);
        existing.setUserId(9L);
        existing.setMonth("2026-07");
        existing.setTotalIncome(new BigDecimal("7000.00"));
        existing.setTotalExpense(new BigDecimal("4000.00"));
        existing.setSavingsRate(new BigDecimal("42.86"));
        existing.setNetWorthStart(new BigDecimal("100.00"));
        existing.setNetWorthEnd(new BigDecimal("200.00"));
        existing.setSubscriptionSpend(BigDecimal.ZERO);
        existing.setDetail("{\"topAccountChanges\":[],\"incomeTop\":null,\"expenseTop\":null,\"budget\":null}");
        when(monthlyReportMapper.selectByUserIdAndMonth(9L, "2026-07")).thenReturn(existing);

        MonthlyReportDetailVO d = service.generate(9L, "2026-07");
        assertEquals(7L, d.getId(), "应回读既有快照");
        assertDecimal("7000.00", d.getTotalIncome());
        assertDecimal("42.86", d.getSavingsRate());
        verify(monthlyReportMapper, never()).insert(any(MonthlyReport.class));
        verify(reportMapper, never()).selectMonthlyIncomeExpense(any(), anyString(), anyString());
        verify(notificationService, never()).insertIfAbsent(any());
    }

    @Test
    void U407_储蓄率_正常与零收入() {
        assertDecimal("37.50", service.calcSavingsRate(new BigDecimal("8000"), new BigDecimal("5000")));
        assertDecimal("100.00", service.calcSavingsRate(new BigDecimal("100"), BigDecimal.ZERO));
        assertNull(service.calcSavingsRate(BigDecimal.ZERO, new BigDecimal("100")), "零收入应返回 null");
    }

    @Test
    void U408_预算执行_有预算聚合与超支判定() {
        MonthlyReportDetailVO.BudgetExecution exec = service.buildBudgetExecution(List.of(
                budgetVo(12L, "餐饮", "2000.00", "1800.00", "90.00"),
                budgetVo(13L, "交通", "500.00", "600.00", "120.00")));
        assertNotNull(exec);
        assertDecimal("96.00", exec.getRate());
        assertEquals(2, exec.getItems().size());
        MonthlyReportDetailVO.BudgetItem food = exec.getItems().get(0);
        MonthlyReportDetailVO.BudgetItem traffic = exec.getItems().get(1);
        assertEquals("餐饮", food.getName());
        assertDecimal("1800.00", food.getSpent());
        assertDecimal("2000.00", food.getBudget());
        assertDecimal("90.00", food.getUsagePercent());
        assertFalse(food.getExceeded());
        assertDecimal("120.00", traffic.getUsagePercent());
        assertTrue(traffic.getExceeded());
    }

    @Test
    void U408_预算执行_未设预算返回null() {
        assertNull(service.buildBudgetExecution(List.of()));
        assertNull(service.buildBudgetExecution(null));
    }
}
