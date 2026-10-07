package com.duoduocode.service.report;

import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.report.entity.MonthlyReport;
import com.duoduocode.service.report.mapper.MonthlyReportMapper;
import com.duoduocode.service.report.service.MonthlyReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * 月报调度器（R004，S4-Q3 裁决：独立调度器每月 1 日 05:00）
 * 扫上月有交易的用户逐一 generate + 发通知；逐用户 try/catch 隔离，单用户异常不中断整轮。
 */
@Slf4j
@Component
public class MonthlyReportScheduler {

    private final MonthlyReportMapper monthlyReportMapper;
    private final MonthlyReportService monthlyReportService;
    private final NotificationService notificationService;

    public MonthlyReportScheduler(MonthlyReportMapper monthlyReportMapper,
                                  MonthlyReportService monthlyReportService,
                                  NotificationService notificationService) {
        this.monthlyReportMapper = monthlyReportMapper;
        this.monthlyReportService = monthlyReportService;
        this.notificationService = notificationService;
    }

    /**
     * 每月 1 日 05:00 生成上月月报
     */
    @Scheduled(cron = "${app.report.monthly-cron:0 0 5 1 * ?}")
    public void scheduledGenerate() {
        generateLastMonthReports();
    }

    /**
     * 执行一轮上月月报生成（定时任务与测试入口共用）
     */
    public void generateLastMonthReports() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        List<Long> userIds = monthlyReportMapper.selectUserIdsWithTransactions(lastMonth);
        if (userIds == null || userIds.isEmpty()) {
            log.info("[月报调度] 上月无交易用户，本轮跳过: month={}", lastMonth);
            return;
        }
        int ok = 0;
        for (Long userId : userIds) {
            try {
                monthlyReportService.generate(userId, lastMonth);
                MonthlyReport report = monthlyReportMapper.selectByUserIdAndMonth(userId, lastMonth);
                if (report != null) {
                    notificationService.insertIfAbsent(
                            notificationService.buildMonthlyReportNotification(report));
                }
                ok++;
            } catch (Exception e) {
                log.warn("[月报调度] 单用户生成失败，跳过继续: userId={}, month={}", userId, lastMonth, e);
            }
        }
        log.info("[月报调度] 完成: month={}, users={}, ok={}", lastMonth, userIds.size(), ok);
    }
}
