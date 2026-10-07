package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 预算使用率 → 通知判定（实时钩子与每日兜底扫描共用）。
 * 纯判定逻辑（level/usagePercent/buildContent）与 Spring 无依赖，可独立单测。
 */
@Slf4j
@Component
public class BudgetNoticeChecker {

    /**
     * 通知级别
     */
    public enum Level { NONE, WARN, EXCEED }

    @Value("${app.notification.budget-warn-threshold:80}")
    private BigDecimal warnThreshold = new BigDecimal("80");

    @Value("${app.notification.budget-exceed-threshold:100}")
    private BigDecimal exceedThreshold = new BigDecimal("100");

    @Autowired(required = false)
    private DailyBudgetMapper dailyBudgetMapper;

    @Autowired(required = false)
    private BudgetCarryoverMapper budgetCarryoverMapper;

    @Autowired(required = false)
    private CategoryMapper categoryMapper;

    @Autowired(required = false)
    private EntryMapper entryMapper;

    @Autowired(required = false)
    private NotificationService notificationService;

    // ===== 纯判定逻辑（单测覆盖） =====

    /**
     * 使用率 → 通知级别
     */
    public Level level(BigDecimal usagePercent) {
        if (usagePercent == null) {
            return Level.NONE;
        }
        if (usagePercent.compareTo(exceedThreshold) >= 0) {
            return Level.EXCEED;
        }
        if (usagePercent.compareTo(warnThreshold) >= 0) {
            return Level.WARN;
        }
        return Level.NONE;
    }

    /**
     * 使用率计算（含结转口径）：spent / (budget + carryover) * 100，保留两位
     * 预算为 0 且有支出 → 视为超限（返回 100+）
     */
    public BigDecimal usagePercent(BigDecimal spent, BigDecimal budget, BigDecimal carryover) {
        BigDecimal s = spent == null ? BigDecimal.ZERO : spent;
        BigDecimal available = (budget == null ? BigDecimal.ZERO : budget)
                .add(carryover == null ? BigDecimal.ZERO : carryover);
        if (available.compareTo(BigDecimal.ZERO) > 0) {
            return s.divide(available, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (s.compareTo(BigDecimal.ZERO) > 0) {
            return new BigDecimal("100.00");
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 通知文案（含量与百分比）
     */
    public String buildContent(String categoryName, BigDecimal usagePercent,
                               BigDecimal spent, BigDecimal budget) {
        String percent = usagePercent == null
                ? "-" : usagePercent.setScale(0, RoundingMode.HALF_UP).toPlainString();
        return "「" + categoryName + "」预算已用 " + percent + "%（¥"
                + NotificationService.stripZero(spent) + "/¥"
                + NotificationService.stripZero(budget) + "）";
    }

    // ===== 运行时钩子 =====

    /**
     * 记账/编辑/删除成功路径调用：事务提交后按分类补查预算使用率并落通知。
     * 通知失败只打日志，不影响记账主流程。
     */
    public void checkAfterTransaction(Long userId, List<Long> categoryIds) {
        if (userId == null || categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务上下文（理论不会发生）直接同步检查
            doCheck(userId, categoryIds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    doCheck(userId, categoryIds);
                } catch (Exception e) {
                    log.warn("预算通知检查失败（不影响记账）: userId={}, categoryIds={}", userId, categoryIds, e);
                }
            }
        });
    }

    /**
     * 当月有预算的分类逐个算 usagePercent，跨阈值则幂等落通知（每日兜底扫描入口）
     *
     * @return 新增通知条数
     */
    public int scanMonthFallback() {
        if (dailyBudgetMapper == null || notificationService == null) {
            return 0;
        }
        String month = YearMonth.now().toString();
        List<DailyBudget> budgets = dailyBudgetMapper.selectByMonth(month);
        int created = 0;
        List<Long> categoryIds = new ArrayList<>();
        for (DailyBudget budget : budgets) {
            categoryIds.add(budget.getCategoryId());
        }
        for (DailyBudget budget : budgets) {
            if (budget.getMonthlyBudget() == null
                    || budget.getMonthlyBudget().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                created += checkOne(budget.getUserId(), budget.getCategoryId(), month);
            } catch (Exception e) {
                log.warn("预算兜底通知检查失败: userId={}, categoryId={}",
                        budget.getUserId(), budget.getCategoryId(), e);
            }
        }
        return created;
    }

    private void doCheck(Long userId, List<Long> categoryIds) {
        String month = YearMonth.now().toString();
        for (Long categoryId : categoryIds) {
            try {
                checkOne(userId, categoryId, month);
            } catch (Exception e) {
                log.warn("预算通知检查失败（不影响记账）: userId={}, categoryId={}", userId, categoryId, e);
            }
        }
    }

    private int checkOne(Long userId, Long categoryId, String month) {
        if (dailyBudgetMapper == null || notificationService == null) {
            return 0;
        }
        DailyBudget budget = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(userId, categoryId, month);
        if (budget == null || budget.getMonthlyBudget() == null
                || budget.getMonthlyBudget().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        Category category = categoryMapper == null ? null : categoryMapper.selectById(categoryId);
        String categoryName = category != null ? category.getName() : "分类";

        LocalDate monthStart = YearMonth.parse(month).atDay(1);
        LocalDate monthEnd = YearMonth.parse(month).atEndOfMonth();
        BigDecimal spent = entryMapper == null ? BigDecimal.ZERO
                : entryMapper.sumDebitByCategoryIdAndDateRange(categoryId, monthStart, monthEnd);
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }
        BigDecimal carryover = budgetCarryoverMapper == null ? BigDecimal.ZERO
                : budgetCarryoverMapper.sumCarryoverAmountByCategoryIdAndToMonth(categoryId, month);
        if (carryover == null) {
            carryover = BigDecimal.ZERO;
        }

        BigDecimal percent = usagePercent(spent, budget.getMonthlyBudget(), carryover);
        Level lvl = level(percent);
        if (lvl == Level.NONE) {
            return 0;
        }

        String levelKey = lvl == Level.EXCEED ? "exceed" : "warn";
        Notification notice = notificationService.buildBudgetNotice(
                userId, categoryName, categoryId, month, levelKey,
                percent, spent, budget.getMonthlyBudget());
        return notificationService.insertIfAbsent(notice) ? 1 : 0;
    }
}
