package com.duoduocode.service.notification.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.notification.dto.NotificationListVO;
import com.duoduocode.service.notification.dto.NotificationReadDTO;
import com.duoduocode.service.notification.dto.NotificationVO;
import com.duoduocode.service.notification.entity.Notification;
import com.duoduocode.service.notification.mapper.NotificationMapper;
import com.duoduocode.service.notification.push.NotificationPublisher;
import com.duoduocode.service.recalculation.entity.RecalculationTask;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.report.entity.MonthlyReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息/待办服务
 * 幂等创建（dedup_key 唯一约束）、列表/已读/未读数、待办消化
 */
@Slf4j
@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationMapper notificationMapper;

    @Autowired(required = false)
    private NotificationPublisher publisher;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    // ===== 幂等键规则 =====

    public static String dedupKeyRecurring(Long templateId, LocalDate nextTriggerDate) {
        return "recur:" + templateId + ":" + nextTriggerDate;
    }

    public static String dedupKeyBudget(Long categoryId, String month, String level) {
        return "budget:" + categoryId + ":" + month + ":" + level;
    }

    public static String dedupKeyRepayment(Long accountId, String month) {
        return "repay:" + accountId + ":" + month;
    }

    // ===== 幂等创建 =====

    /**
     * 幂等创建：dedup_key 已存在时不重复插入；新插入时经推送适配器外发
     */
    public Notification createIfAbsent(Notification notification) {
        insertIfAbsent(notification);
        return notification;
    }

    /**
     * 幂等创建并返回是否新插入（true=本次新落库）
     */
    public boolean insertIfAbsent(Notification notification) {
        int inserted = notificationMapper.insertIgnore(notification);
        if (inserted > 0 && publisher != null) {
            publisher.publish(notification);
        }
        return inserted > 0;
    }

    // ===== 事件构造 =====

    /**
     * 周期到期待办
     */
    public Notification buildRecurringTodo(RecurringTemplate template) {
        LocalDate next = template.getNextTriggerDate();
        Notification n = base(userIdOf(template), "todo", "recurring_due");
        n.setRefType("template");
        n.setRefId(template.getId());
        n.setDedupKey(dedupKeyRecurring(template.getId(), next));
        n.setTitle("「" + template.getName() + "」" + dueText(next));
        n.setContent("¥" + stripZero(template.getAmount()) + " · " + frequencyText(template.getFrequency()));
        return n;
    }

    /**
     * 还款日通知
     *
     * @param debt 本期待还金额（非负）
     */
    public Notification buildRepaymentNotice(Account account, BigDecimal debt, String month) {
        Notification n = base(account.getUserId(), "notice", "repayment_due");
        n.setRefType("account");
        n.setRefId(account.getId());
        n.setDedupKey(dedupKeyRepayment(account.getId(), month));
        n.setTitle("「" + account.getName() + "」还款日提醒");
        String debtText = debt.compareTo(BigDecimal.ZERO) > 0
                ? "本期待还 ¥" + stripZero(debt) : "本期账单已结清";
        n.setContent(debtText + " · 还款日 " + account.getRepaymentDay() + " 日");
        return n;
    }

    /**
     * 预算预警/超限通知
     *
     * @param level warn/exceed
     */
    public Notification buildBudgetNotice(Long userId, String categoryName, Long categoryId,
                                          String month, String level,
                                          BigDecimal usagePercent, BigDecimal spent, BigDecimal budget) {
        boolean exceed = "exceed".equals(level);
        Notification n = base(userId, "notice", exceed ? "budget_exceeded" : "budget_warning");
        n.setRefType("category");
        n.setRefId(categoryId);
        n.setDedupKey(dedupKeyBudget(categoryId, month, level));
        n.setTitle(exceed ? "「" + categoryName + "」预算已超限" : "「" + categoryName + "」预算预警");
        n.setContent(budgetContent(usagePercent, spent, budget, exceed));
        return n;
    }

    /**
     * 批量誊录任务完成通知（R003）：dedup_key=task:{id}，任务级只发一条
     */
    public Notification buildTaskDoneNotification(RecalculationTask task) {
        boolean isImport = "import".equals(task.getTaskType());
        Notification n = base(task.getUserId(), "notice", "task_done");
        n.setRefType("task");
        n.setRefId(task.getId());
        n.setDedupKey("task:" + task.getId());
        n.setTitle(isImport ? "账单导入完成，账目已更新" : "批量确认完成，账目已更新");
        StringBuilder content = new StringBuilder();
        content.append(isImport ? "已导入 " : "已确认 ")
                .append(task.getSuccessCount() == null ? 0 : task.getSuccessCount())
                .append(" 笔");
        if (task.getFailCount() != null && task.getFailCount() > 0) {
            content.append("，失败 ").append(task.getFailCount()).append(" 笔");
        }
        n.setContent(content.toString());
        return n;
    }

    /**
     * 月报生成通知（R004）：dedup_key=report:{userId}:{month}，refId=月报ID 直达详情
     */
    public Notification buildMonthlyReportNotification(MonthlyReport report) {
        Notification n = base(report.getUserId(), "notice", "monthly_report");
        n.setRefType("report");
        n.setRefId(report.getId());
        n.setDedupKey("report:" + report.getUserId() + ":" + report.getMonth());
        n.setTitle(monthNumberOf(report) + "月财务月报已生成");
        n.setContent(monthlyReportContent(report));
        return n;
    }

    private int monthNumberOf(MonthlyReport report) {
        String month = report.getMonth();
        if (month == null || month.length() < 7) {
            return 0;
        }
        try {
            return Integer.parseInt(month.substring(5, 7));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String monthlyReportContent(MonthlyReport report) {
        StringBuilder sb = new StringBuilder();
        if (report.getTotalIncome() != null || report.getTotalExpense() != null) {
            sb.append("收入 ¥").append(stripZero(nullSafe(report.getTotalIncome())))
                    .append(" · 支出 ¥").append(stripZero(nullSafe(report.getTotalExpense())));
        }
        if (report.getSavingsRate() != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("储蓄率 ").append(stripZero(report.getSavingsRate())).append("%");
        }
        BigDecimal change = null;
        if (report.getNetWorthStart() != null && report.getNetWorthEnd() != null) {
            change = report.getNetWorthEnd().subtract(report.getNetWorthStart());
        }
        if (change != null) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append("净资产变动 ").append(change.signum() >= 0 ? "+" : "")
                    .append(stripZero(change));
        }
        if (sb.length() == 0) {
            sb.append("点击查看本期月报");
        }
        return sb.toString();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String budgetContent(BigDecimal usagePercent,
                                 BigDecimal spent, BigDecimal budget, boolean exceed) {
        String percent = usagePercent == null ? "-" : usagePercent.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
        String head = exceed ? "已用 " : "已用 ";
        return head + percent + "%（¥" + stripZero(spent) + "/¥" + stripZero(budget) + "），"
                + (exceed ? "超出预算，建议暂停非必要支出" : "接近预算上限，注意支出节奏");
    }

    private Notification base(Long userId, String msgType, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMsgType(msgType);
        n.setType(type);
        n.setStatus("unread");
        return n;
    }

    private Long userIdOf(RecurringTemplate template) {
        return template.getUserId();
    }

    private String dueText(LocalDate next) {
        LocalDate today = LocalDate.now();
        if (next == null || next.isBefore(today)) {
            return "待确认";
        }
        if (next.isEqual(today)) {
            return "今日到期";
        }
        if (next.isEqual(today.plusDays(1))) {
            return "明日到期";
        }
        return next.getMonthValue() + "月" + next.getDayOfMonth() + "日到期";
    }

    private String frequencyText(String frequency) {
        if (frequency == null) {
            return "周期";
        }
        switch (frequency) {
            case "daily": return "每天";
            case "weekly": return "每周";
            case "monthly": return "每月";
            case "yearly": return "每年";
            default: return frequency;
        }
    }

    public static String stripZero(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    // ===== 查询与状态 =====

    /**
     * 分页查询消息列表（待办置顶），附带未读数统计
     */
    public NotificationListVO getList(Long userId, Integer page, Integer pageSize, String msgType) {
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 100);
        String mt = normalizeMsgType(msgType);

        List<Notification> list = notificationMapper.selectPage(userId, (p - 1) * size, size, mt);
        Long total = notificationMapper.countByUserId(userId, mt);

        List<NotificationVO> items = new ArrayList<>();
        for (Notification n : list) {
            items.add(convertToVO(n));
        }

        NotificationListVO vo = new NotificationListVO();
        vo.setItems(items);
        vo.setTotal(total != null ? total : 0L);
        vo.setPage(p);
        vo.setPageSize(size);
        vo.setUnreadCount(getUnreadCount(userId));
        return vo;
    }

    /**
     * 标记已读：all=true 仅通知型生效；ids 仅通知型生效（待办以完成动作消化）
     */
    public int markRead(Long userId, NotificationReadDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请求体不能为空");
        }
        if (Boolean.TRUE.equals(dto.getAll())) {
            return notificationMapper.markAllNoticeRead(userId);
        }
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "ids 与 all 不能同时为空");
        }
        return notificationMapper.markRead(userId, dto.getIds());
    }

    /**
     * 未读数：通知未读 + 待办未消化
     */
    public Map<String, Long> getUnreadCount(Long userId) {
        Map<String, Long> result = new HashMap<>();
        result.put("todo", 0L);
        result.put("notice", 0L);
        long total = 0L;
        for (Map<String, Object> row : notificationMapper.countUnreadByMsgType(userId)) {
            String mt = (String) row.get("msgType");
            Object cnt = row.get("cnt");
            if (mt != null && cnt instanceof Number) {
                result.put(mt, ((Number) cnt).longValue());
                total += ((Number) cnt).longValue();
            }
        }
        result.put("total", total);
        return result;
    }

    /**
     * 消化指定模板某一期（dedup_key = recur:{templateId}:{cycleDate}）的待办。
     * 只消化被确认的那一期，未来期次的预告待办不受牵连。
     */
    public int digestRecurringTodo(Long userId, Long templateId, LocalDate cycleDate) {
        if (userId == null || templateId == null || cycleDate == null) {
            return 0;
        }
        return notificationMapper.digestByDedupKey(userId, dedupKeyRecurring(templateId, cycleDate));
    }

    private String normalizeMsgType(String msgType) {
        if (msgType == null || msgType.trim().isEmpty() || "all".equals(msgType.trim())) {
            return null;
        }
        return msgType.trim();
    }

    private NotificationVO convertToVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setMsgType(n.getMsgType());
        vo.setType(n.getType());
        vo.setTitle(n.getTitle());
        vo.setContent(n.getContent());
        vo.setRefType(n.getRefType());
        vo.setRefId(n.getRefId());
        vo.setStatus(n.getStatus());
        vo.setCreatedAt(n.getCreatedAt());
        return vo;
    }
}
