package com.duoduocode.service.notification.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.budget.service.BudgetNoticeChecker;
import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.mapper.NotificationMapper;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日通知扫描器：周期到期待办 / 还款日通知 / 预算兜底通知 + 保留期清理。
 * 幂等由 notification 表 uk_user_dedup 唯一约束保证（重跑不重复）。
 */
@Slf4j
@Component
public class NotificationScheduler {

    @Value("${app.notification.repayment-advance-days:2}")
    private int repaymentAdvanceDays = 2;

    @Value("${app.notification.notice-retention-days:90}")
    private int noticeRetentionDays = 90;

    @Value("${app.notification.todo-retention-days:30}")
    private int todoRetentionDays = 30;

    @Autowired(required = false)
    private RecurringTemplateMapper recurringTemplateMapper;

    @Autowired(required = false)
    private AccountMapper accountMapper;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private BudgetNoticeChecker budgetNoticeChecker;

    @Autowired(required = false)
    private NotificationMapper notificationMapper;

    /**
     * 每日 08:30 扫描三类事件
     */
    @Scheduled(cron = "${app.notification.scan-cron:0 30 8 * * ?}")
    public void scanDaily() {
        Map<String, Integer> stats = scanOnce();
        log.info("[通知扫描] 完成: {}", stats);
    }

    /**
     * 执行一轮完整扫描（定时任务与测试入口共用）
     *
     * @return 各事件新增条数
     */
    public Map<String, Integer> scanOnce() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("recurring", scanRecurringDue());
        stats.put("repayment", scanRepaymentDue());
        stats.put("budget", scanBudgetFallback());
        return stats;
    }

    /**
     * 每日 08:40 清理过期消息：已读通知保留 90 天、已消化待办保留 30 天
     */
    @Scheduled(cron = "${app.notification.cleanup-cron:0 40 8 * * ?}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        int notices = notificationMapper == null ? 0
                : notificationMapper.deleteExpiredReadNotices(now.minusDays(noticeRetentionDays));
        int todos = notificationMapper == null ? 0
                : notificationMapper.deleteExpiredDigestedTodos(now.minusDays(todoRetentionDays));
        log.info("[通知清理] 删除过期通知 {} 条、过期待办 {} 条", notices, todos);
    }

    // ===== 扫描逻辑 =====

    /**
     * ① 周期到期：active 且 next_trigger_date ≤ 明天 → todo
     */
    private int scanRecurringDue() {
        if (recurringTemplateMapper == null || notificationService == null) {
            return 0;
        }
        LocalDate maxDate = LocalDate.now().plusDays(1);
        List<RecurringTemplate> templates = recurringTemplateMapper.selectDueTemplatesAll(maxDate);
        int created = 0;
        for (RecurringTemplate template : templates) {
            try {
                if (notificationService.insertIfAbsent(notificationService.buildRecurringTodo(template))) {
                    created++;
                }
            } catch (Exception e) {
                log.warn("周期到期待办生成失败: templateId={}", template.getId(), e);
            }
        }
        return created;
    }

    /**
     * ② 还款日：liability 且还款日命中提醒日 → notice（含欠款额）
     */
    private int scanRepaymentDue() {
        if (accountMapper == null || notificationService == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        String month = YearMonth.now().toString();
        List<Account> accounts = accountMapper.selectLiabilityWithRepayment();
        int created = 0;
        for (Account account : accounts) {
            try {
                if (!shouldNotifyToday(account.getRepaymentDay(), today, repaymentAdvanceDays)) {
                    continue;
                }
                BigDecimal debt = BigDecimal.ZERO;
                if (account.getCurrentBalance() != null
                        && account.getCurrentBalance().compareTo(BigDecimal.ZERO) < 0) {
                    debt = account.getCurrentBalance().negate();
                }
                if (notificationService.insertIfAbsent(
                        notificationService.buildRepaymentNotice(account, debt, month))) {
                    created++;
                }
            } catch (Exception e) {
                log.warn("还款日通知生成失败: accountId={}", account.getId(), e);
            }
        }
        return created;
    }

    /**
     * ③ 预算兜底：当月有预算的分类逐个算使用率（与实时钩子同一 dedup，不重复）
     */
    private int scanBudgetFallback() {
        return budgetNoticeChecker == null ? 0 : budgetNoticeChecker.scanMonthFallback();
    }

    // ===== 纯日期算法（单测覆盖） =====

    /**
     * 还款提醒日：还款日按目标月天数收敛（31 日在 2 月收敛为 28/29），再前移提前天数
     */
    public LocalDate repaymentNotifyDate(Integer repaymentDay, LocalDate today, int advanceDays) {
        if (repaymentDay == null || repaymentDay <= 0) {
            return null;
        }
        LocalDate target = today.plusDays(advanceDays);
        int effective = Math.min(repaymentDay, target.lengthOfMonth());
        return target.withDayOfMonth(effective).minusDays(advanceDays);
    }

    /**
     * 今天是否为该账户的还款提醒日
     */
    public boolean shouldNotifyToday(Integer repaymentDay, LocalDate today, int advanceDays) {
        LocalDate notifyDate = repaymentNotifyDate(repaymentDay, today, advanceDays);
        return notifyDate != null && notifyDate.equals(today);
    }

    /**
     * 通知是否过期（已读通知保留 90 天）
     */
    public boolean isNoticeExpired(LocalDate createdAt, LocalDate now) {
        return createdAt != null && createdAt.isBefore(now.minusDays(noticeRetentionDays));
    }

    /**
     * 待办是否过期（已消化待办保留 30 天）
     */
    public boolean isTodoExpired(LocalDate createdAt, LocalDate now) {
        return createdAt != null && createdAt.isBefore(now.minusDays(todoRetentionDays));
    }
}
