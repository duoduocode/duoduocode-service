package com.duoduocode.service.report.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.budget.dto.DailyBudgetVO;
import com.duoduocode.service.budget.service.BudgetService;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.report.dto.CategoryData;
import com.duoduocode.service.report.dto.MonthlyData;
import com.duoduocode.service.report.dto.MonthlyReportDetailVO;
import com.duoduocode.service.report.dto.MonthlyReportListVO;
import com.duoduocode.service.report.entity.MonthlyReport;
import com.duoduocode.service.report.mapper.MonthlyReportMapper;
import com.duoduocode.service.report.mapper.ReportMapper;
import com.duoduocode.service.security.SecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 月度财务报告服务（R004）
 * generate 幂等（uk_user_month）、快照=生成时点数据、通知失败不影响快照落库
 */
@Slf4j
@Service
public class MonthlyReportService {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 归因 top 条数
     */
    private static final int TOP_ACCOUNT_LIMIT = 3;

    /**
     * 列表窗口上限
     */
    private static final int MAX_LIST_MONTHS = 12;

    private final MonthlyReportMapper monthlyReportMapper;
    private final ReportMapper reportMapper;
    private final BudgetService budgetService;
    private final NotificationService notificationService;

    public MonthlyReportService(MonthlyReportMapper monthlyReportMapper,
                                ReportMapper reportMapper,
                                BudgetService budgetService,
                                NotificationService notificationService) {
        this.monthlyReportMapper = monthlyReportMapper;
        this.reportMapper = reportMapper;
        this.budgetService = budgetService;
        this.notificationService = notificationService;
    }

    /**
     * 生成（或幂等回读）指定完整月的月报快照
     *
     * @param userId 用户ID
     * @param month  月份 YYYY-MM（须为近 12 个完整月内）
     */
    public MonthlyReportDetailVO generate(Long userId, String month) {
        YearMonth ym = requireCompleteMonth(month);

        MonthlyReport existing = monthlyReportMapper.selectByUserIdAndMonth(userId, month);
        if (existing != null) {
            return toDetailVO(existing);
        }

        String startDate = ym.atDay(1).toString();
        String endDate = ym.atEndOfMonth().toString();

        List<MonthlyData> monthlyData = reportMapper.selectMonthlyIncomeExpense(userId, startDate, endDate);
        if (monthlyData == null || monthlyData.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该月无账目可誊录");
        }
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (MonthlyData data : monthlyData) {
            if (data.getIncome() != null) {
                totalIncome = totalIncome.add(data.getIncome());
            }
            if (data.getExpense() != null) {
                totalExpense = totalExpense.add(data.getExpense());
            }
        }

        List<Account> startAccounts = monthlyReportMapper.selectAccountBalancesAsOf(userId, startDate);
        List<Account> endAccounts = monthlyReportMapper.selectAccountBalancesAsOf(userId, endDate);
        BigDecimal netWorthStart = netWorthOf(startAccounts);
        BigDecimal netWorthEnd = netWorthOf(endAccounts);

        List<MonthlyReportDetailVO.AccountChange> topChanges =
                buildTopAccountChanges(startAccounts, endAccounts);
        MonthlyReportDetailVO.CategoryTop incomeTop =
                firstTop(reportMapper.selectCategoryStats(userId, "income", startDate, endDate));
        MonthlyReportDetailVO.CategoryTop expenseTop =
                firstTop(reportMapper.selectCategoryStats(userId, "expense", startDate, endDate));
        MonthlyReportDetailVO.BudgetExecution budget =
                buildBudgetExecution(budgetService.getDailyBudgetUsage(userId, month));
        BigDecimal subscriptionSpend = monthlyReportMapper.selectSubscriptionSpend(userId, startDate, endDate);
        if (subscriptionSpend == null) {
            subscriptionSpend = BigDecimal.ZERO;
        }

        MonthlyReport report = new MonthlyReport();
        report.setUserId(userId);
        report.setMonth(month);
        report.setTotalIncome(totalIncome);
        report.setTotalExpense(totalExpense);
        report.setSavingsRate(calcSavingsRate(totalIncome, totalExpense));
        report.setNetWorthStart(netWorthStart);
        report.setNetWorthEnd(netWorthEnd);
        report.setSubscriptionSpend(subscriptionSpend);
        report.setDetail(serializeDetail(topChanges, incomeTop, expenseTop, budget));

        try {
            monthlyReportMapper.insert(report);
        } catch (DuplicateKeyException e) {
            // 并发兜底：手动与调度同时生成时，冲突方回读既有快照
            MonthlyReport raced = monthlyReportMapper.selectByUserIdAndMonth(userId, month);
            if (raced != null) {
                return toDetailVO(raced);
            }
            throw e;
        }

        notifyGenerated(report);
        return toDetailVO(report);
    }

    /**
     * 月报详情（不存在或非本人 → 404，不泄露存在性）
     */
    public MonthlyReportDetailVO getDetail(Long reportId) {
        MonthlyReport report = monthlyReportMapper.selectById(reportId);
        Long currentUserId = SecurityContext.getUserId();
        if (report == null
                || (currentUserId != null && !currentUserId.equals(report.getUserId()))) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "月报不存在");
        }
        return toDetailVO(report);
    }

    /**
     * 月报列表：近 N 个完整月倒序（不含当月），已生成/未生成两态行
     */
    public MonthlyReportListVO list(Long userId, int months) {
        int window = Math.min(Math.max(months, 1), MAX_LIST_MONTHS);
        YearMonth current = YearMonth.now();

        Map<String, MonthlyReport> byMonth = new HashMap<>();
        List<MonthlyReport> reports =
                monthlyReportMapper.selectByUserIdFromMonth(userId, current.minusMonths(window).toString());
        if (reports != null) {
            for (MonthlyReport r : reports) {
                byMonth.put(r.getMonth(), r);
            }
        }

        List<MonthlyReportListVO.Row> rows = new ArrayList<>(window);
        for (int i = 1; i <= window; i++) {
            String month = current.minusMonths(i).toString();
            MonthlyReportListVO.Row row = new MonthlyReportListVO.Row();
            row.setMonth(month);
            MonthlyReport r = byMonth.get(month);
            if (r != null) {
                row.setId(r.getId());
                row.setGenerated(true);
                row.setNetWorthChange(netWorthChangeOf(r));
                row.setSavingsRate(r.getSavingsRate());
            } else {
                row.setGenerated(false);
            }
            rows.add(row);
        }

        MonthlyReportListVO vo = new MonthlyReportListVO();
        vo.setRows(rows);
        return vo;
    }

    /**
     * 储蓄率% = (收入 − 支出) ÷ 收入 × 100，HALF_UP 2 位；收入为 0 → null
     */
    public BigDecimal calcSavingsRate(BigDecimal totalIncome, BigDecimal totalExpense) {
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal expense = totalExpense == null ? BigDecimal.ZERO : totalExpense;
        return totalIncome.subtract(expense)
                .multiply(BigDecimal.valueOf(100))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * 预算执行聚合：总执行率 = 已用合计 ÷ 预算合计 × 100（HALF_UP 2 位）；未设预算 → null
     */
    public MonthlyReportDetailVO.BudgetExecution buildBudgetExecution(List<DailyBudgetVO> budgetItems) {
        if (budgetItems == null || budgetItems.isEmpty()) {
            return null;
        }
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        List<MonthlyReportDetailVO.BudgetItem> items = new ArrayList<>();
        for (DailyBudgetVO vo : budgetItems) {
            if (vo == null || vo.getMonthlyBudget() == null
                    || vo.getMonthlyBudget().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal spent = vo.getUsedAmount() == null ? BigDecimal.ZERO : vo.getUsedAmount();
            totalBudget = totalBudget.add(vo.getMonthlyBudget());
            totalUsed = totalUsed.add(spent);

            MonthlyReportDetailVO.BudgetItem item = new MonthlyReportDetailVO.BudgetItem();
            item.setCategoryId(vo.getCategoryId());
            item.setName(vo.getCategoryName());
            item.setSpent(spent);
            item.setBudget(vo.getMonthlyBudget());
            item.setUsagePercent(vo.getUsagePercent());
            item.setExceeded(spent.compareTo(vo.getMonthlyBudget()) > 0);
            items.add(item);
        }
        if (items.isEmpty()) {
            return null;
        }
        MonthlyReportDetailVO.BudgetExecution exec = new MonthlyReportDetailVO.BudgetExecution();
        exec.setRate(totalUsed.multiply(BigDecimal.valueOf(100))
                .divide(totalBudget, 2, RoundingMode.HALF_UP));
        exec.setItems(items);
        return exec;
    }

    // ===== 私有辅助 =====

    /**
     * month 格式与范围校验：YYYY-MM 且在近 12 个完整月内（当月/未来/13 个月前 → 400）
     */
    private YearMonth requireCompleteMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "月份格式应为 YYYY-MM");
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "月份格式应为 YYYY-MM");
        }
        YearMonth current = YearMonth.now();
        if (!ym.isBefore(current) || ym.isBefore(current.minusMonths(MAX_LIST_MONTHS))) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "仅支持生成近 " + MAX_LIST_MONTHS + " 个完整月的月报");
        }
        return ym;
    }

    /**
     * 净资产 = 计入净资产的账户余额合计（asset/investment 为正、liability 为负）
     */
    private BigDecimal netWorthOf(List<Account> accounts) {
        BigDecimal net = BigDecimal.ZERO;
        if (accounts == null) {
            return net;
        }
        for (Account a : accounts) {
            if (!Boolean.TRUE.equals(a.getIncludeInNetWorth())) {
                continue;
            }
            net = net.add(a.getCurrentBalance() == null ? BigDecimal.ZERO : a.getCurrentBalance());
        }
        return net;
    }

    /**
     * 净资产归因：期末 − 期初，按变动绝对值降序取 top（剔除零变动）
     */
    private List<MonthlyReportDetailVO.AccountChange> buildTopAccountChanges(List<Account> startAccounts,
                                                                              List<Account> endAccounts) {
        Map<Long, BigDecimal> startById = balancesById(startAccounts);
        Map<Long, BigDecimal> endById = balancesById(endAccounts);

        Map<Long, String> names = new LinkedHashMap<>();
        if (startAccounts != null) {
            for (Account a : startAccounts) {
                if (Boolean.TRUE.equals(a.getIncludeInNetWorth())) {
                    names.putIfAbsent(a.getId(), a.getName());
                }
            }
        }
        if (endAccounts != null) {
            for (Account a : endAccounts) {
                if (Boolean.TRUE.equals(a.getIncludeInNetWorth())) {
                    names.putIfAbsent(a.getId(), a.getName());
                }
            }
        }

        List<MonthlyReportDetailVO.AccountChange> changes = new ArrayList<>();
        for (Map.Entry<Long, String> entry : names.entrySet()) {
            Long id = entry.getKey();
            BigDecimal start = startById.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal end = endById.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal change = end.subtract(start);
            if (change.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            MonthlyReportDetailVO.AccountChange c = new MonthlyReportDetailVO.AccountChange();
            c.setAccountId(id);
            c.setName(entry.getValue());
            c.setChange(change);
            changes.add(c);
        }
        changes.sort(Comparator.comparing((MonthlyReportDetailVO.AccountChange c) ->
                        c.getChange().abs()).reversed());
        return changes.size() > TOP_ACCOUNT_LIMIT
                ? new ArrayList<>(changes.subList(0, TOP_ACCOUNT_LIMIT)) : changes;
    }

    private Map<Long, BigDecimal> balancesById(List<Account> accounts) {
        Map<Long, BigDecimal> byId = new HashMap<>();
        if (accounts != null) {
            for (Account a : accounts) {
                if (Boolean.TRUE.equals(a.getIncludeInNetWorth())) {
                    byId.put(a.getId(), a.getCurrentBalance() == null
                            ? BigDecimal.ZERO : a.getCurrentBalance());
                }
            }
        }
        return byId;
    }

    private MonthlyReportDetailVO.CategoryTop firstTop(List<CategoryData> stats) {
        if (stats == null || stats.isEmpty() || stats.get(0) == null) {
            return null;
        }
        CategoryData first = stats.get(0);
        if (first.getAmount() == null) {
            return null;
        }
        MonthlyReportDetailVO.CategoryTop top = new MonthlyReportDetailVO.CategoryTop();
        top.setCategoryId(first.getCategoryId());
        top.setName(first.getCategoryName());
        top.setAmount(first.getAmount());
        return top;
    }

    private BigDecimal netWorthChangeOf(MonthlyReport report) {
        BigDecimal start = report.getNetWorthStart() == null ? BigDecimal.ZERO : report.getNetWorthStart();
        BigDecimal end = report.getNetWorthEnd() == null ? BigDecimal.ZERO : report.getNetWorthEnd();
        return end.subtract(start);
    }

    /**
     * 快照落库后发月报通知；通知失败仅告警，不影响快照
     */
    private void notifyGenerated(MonthlyReport report) {
        try {
            Notification notification = notificationService.buildMonthlyReportNotification(report);
            notificationService.insertIfAbsent(notification);
        } catch (Exception e) {
            log.warn("[月报] 通知生成失败（快照已落库）: userId={}, month={}", report.getUserId(), report.getMonth(), e);
        }
    }

    private MonthlyReportDetailVO toDetailVO(MonthlyReport report) {
        MonthlyReportDetailVO vo = new MonthlyReportDetailVO();
        vo.setId(report.getId());
        vo.setMonth(report.getMonth());
        vo.setGeneratedAt(report.getCreatedAt());
        vo.setTotalIncome(report.getTotalIncome());
        vo.setTotalExpense(report.getTotalExpense());
        vo.setBalance(report.getTotalIncome() == null || report.getTotalExpense() == null
                ? null : report.getTotalIncome().subtract(report.getTotalExpense()));
        vo.setSavingsRate(report.getSavingsRate());
        vo.setNetWorthStart(report.getNetWorthStart());
        vo.setNetWorthEnd(report.getNetWorthEnd());
        vo.setNetWorthChange(netWorthChangeOf(report));
        vo.setSubscriptionSpend(report.getSubscriptionSpend());

        DetailPayload payload = parseDetail(report.getDetail());
        vo.setTopAccountChanges(payload.topAccountChanges == null
                ? new ArrayList<>() : payload.topAccountChanges);
        vo.setIncomeTop(payload.incomeTop);
        vo.setExpenseTop(payload.expenseTop);
        vo.setBudget(payload.budget);
        return vo;
    }

    private String serializeDetail(List<MonthlyReportDetailVO.AccountChange> topAccountChanges,
                                   MonthlyReportDetailVO.CategoryTop incomeTop,
                                   MonthlyReportDetailVO.CategoryTop expenseTop,
                                   MonthlyReportDetailVO.BudgetExecution budget) {
        DetailPayload payload = new DetailPayload();
        payload.topAccountChanges = topAccountChanges == null
                ? new ArrayList<>() : topAccountChanges;
        payload.incomeTop = incomeTop;
        payload.expenseTop = expenseTop;
        payload.budget = budget;
        try {
            return JSON.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[月报] detail JSON 序列化失败，落空快照", e);
            return "{}";
        }
    }

    private DetailPayload parseDetail(String detail) {
        if (detail == null || detail.trim().isEmpty()) {
            return new DetailPayload();
        }
        try {
            DetailPayload payload = JSON.readValue(detail, DetailPayload.class);
            return payload == null ? new DetailPayload() : payload;
        } catch (Exception e) {
            log.warn("[月报] detail JSON 解析失败: {}", detail, e);
            return new DetailPayload();
        }
    }

    /**
     * detail JSON 快照结构（S4-Q2 裁决：归因/预算/分类top进 JSON）
     */
    public static class DetailPayload {

        public List<MonthlyReportDetailVO.AccountChange> topAccountChanges;
        public MonthlyReportDetailVO.CategoryTop incomeTop;
        public MonthlyReportDetailVO.CategoryTop expenseTop;
        public MonthlyReportDetailVO.BudgetExecution budget;
    }
}
