package com.duoduocode.service.recurring.service;

import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recalculation.service.RecalculationTaskService;
import com.duoduocode.service.recurring.dto.BatchConfirmVO;
import com.duoduocode.service.recurring.dto.ItemResult;
import com.duoduocode.service.recurring.dto.RecurringCalendarVO;
import com.duoduocode.service.recurring.dto.RecurringTemplateDTO;
import com.duoduocode.service.recurring.dto.RecurringTemplateVO;
import com.duoduocode.service.recurring.dto.SubscriptionOverviewVO;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import com.duoduocode.service.transaction.dto.TransactionDTO;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import com.duoduocode.service.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 周期交易模板 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTemplateService {

    private final RecurringTemplateMapper recurringTemplateMapper;
    private final NotificationService notificationService;

    /** 落账走既有记账链路；@Lazy 打破与 TransactionService 的构造循环（单测构造两参即可） */
    @Autowired
    @Lazy
    private TransactionService transactionService;

    /** 批量确认 ≥10 笔转异步任务；@Lazy 打破与 RecalculationTaskService 的循环 */
    @Autowired
    @Lazy
    private RecalculationTaskService recalculationTaskService;

    /** 落账前校验分类/账户有效性（selectById 已滤软删） */
    @Autowired
    @Lazy
    private CategoryMapper categoryMapper;

    @Autowired
    @Lazy
    private AccountMapper accountMapper;

    /** R004 订阅涨价基准：每模板最近一笔交易金额 */
    @Autowired
    @Lazy
    private TransactionMapper transactionMapper;

    private static final int ASYNC_CONFIRM_THRESHOLD = 10;

    /** R004 订阅近 30 天窗口（含端点） */
    private static final int SUBSCRIPTION_WINDOW_DAYS = 30;

    /**
     * 获取周期模板列表
     *
     * @param userId 用户ID
     * @return 模板列表
     */
    public List<RecurringTemplateVO> getTemplateList(Long userId) {
        List<RecurringTemplate> templates = recurringTemplateMapper.selectByUserId(userId);
        return templates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    public RecurringTemplateVO getTemplateDetail(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        return convertToVO(template);
    }

    /**
     * 创建模板
     *
     * @param userId 用户ID
     * @param dto    模板数据
     * @return 创建的模板ID
     */
    @Transactional
    public Long createTemplate(Long userId, RecurringTemplateDTO dto) {
        RecurringTemplate template = new RecurringTemplate();
        template.setUserId(userId);
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setAmount(dto.getAmount());
        template.setCategoryId(dto.getCategoryId());
        template.setAccountId(dto.getAccountId());
        template.setToAccountId(dto.getToAccountId());
        template.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        template.setFrequency(dto.getFrequency());
        template.setDayOfWeek(dto.getDayOfWeek());
        template.setDayOfMonth(dto.getDayOfMonth());
        template.setMonthOfYear(dto.getMonthOfYear());
        template.setStartDate(dto.getStartDate());
        template.setEndDate(dto.getEndDate());
        template.setMaxCount(dto.getMaxCount());
        template.setExecutedCount(0);
        template.setNextTriggerDate(calculateNextTriggerDate(dto));
        template.setStatus("active");
        template.setCreatedAt(LocalDateTime.now());

        recurringTemplateMapper.insert(template);
        return template.getId();
    }

    /**
     * 更新模板
     *
     * @param id  模板ID
     * @param dto 模板数据
     */
    @Transactional
    public void updateTemplate(Long id, RecurringTemplateDTO dto) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setAmount(dto.getAmount());
        template.setCategoryId(dto.getCategoryId());
        template.setAccountId(dto.getAccountId());
        template.setToAccountId(dto.getToAccountId());
        template.setDescription(dto.getDescription());
        template.setFrequency(dto.getFrequency());
        template.setDayOfWeek(dto.getDayOfWeek());
        template.setDayOfMonth(dto.getDayOfMonth());
        template.setMonthOfYear(dto.getMonthOfYear());
        template.setStartDate(dto.getStartDate());
        template.setEndDate(dto.getEndDate());
        template.setMaxCount(dto.getMaxCount());
        template.setNextTriggerDate(calculateNextTriggerDate(dto));

        recurringTemplateMapper.updateById(template);
    }

    /**
     * 删除模板
     *
     * @param id 模板ID
     */
    @Transactional
    public void deleteTemplate(Long id) {
        recurringTemplateMapper.deleteById(id);
    }

    /**
     * 获取到期提醒列表
     *
     * @param userId 用户ID
     * @return 到期模板列表
     */
    public List<RecurringTemplateVO> getDueTemplates(Long userId) {
        LocalDate today = LocalDate.now();
        List<RecurringTemplate> templates = recurringTemplateMapper.selectDueTemplates(userId, today);
        return templates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 手动触发提醒：推进模板 + 消化被确认这一期的待办（R002「跳过」路径副作用）
     *
     * @param id 模板ID
     */
    @Transactional
    public void triggerTemplate(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        advanceTemplate(template);
    }

    /**
     * R003 批量确认：单模板确认（校验 → 落账 → 模板推进+待办消化）。
     * 落账走 createTransaction 的 sourceTemplateId 闭环；单笔失败不影响其余笔。
     */
    public ItemResult confirmSingleTemplate(Long templateId, Long userId) {
        RecurringTemplate template = recurringTemplateMapper.selectById(templateId);
        if (!ownedBy(template, userId)) {
            return ItemResult.fail(templateId, "模板不存在");
        }
        if (!"active".equals(template.getStatus())) {
            return ItemResult.fail(templateId, "模板已暂停或已完成");
        }
        if (template.getCategoryId() != null
                && categoryMapper.selectById(template.getCategoryId()) == null) {
            return ItemResult.fail(templateId, "分类不存在");
        }
        if (template.getAccountId() != null
                && accountMapper.selectById(template.getAccountId()) == null) {
            return ItemResult.fail(templateId, "账户不存在");
        }
        try {
            transactionService.createTransaction(userId, buildConfirmDTO(template));
            return ItemResult.ok(templateId);
        } catch (BusinessException e) {
            return ItemResult.fail(templateId, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("模板 {} 确认落账失败", templateId, e);
            return ItemResult.fail(templateId, "落账失败: " + e.getMessage());
        }
    }

    /**
     * R003 批量跳过：推进模板 + 消化待办，不产生交易
     */
    public ItemResult skipSingleTemplate(Long templateId, Long userId) {
        RecurringTemplate template = recurringTemplateMapper.selectById(templateId);
        if (!ownedBy(template, userId)) {
            return ItemResult.fail(templateId, "模板不存在");
        }
        if (!"active".equals(template.getStatus())) {
            return ItemResult.fail(templateId, "模板已暂停或已完成");
        }
        advanceTemplate(template);
        return ItemResult.ok(templateId);
    }

    /**
     * R003 批量确认编排：勾选 ≥10 笔转异步任务（受理即返 taskId），否则同步逐笔执行
     */
    public BatchConfirmVO batchConfirm(Long userId, List<Long> templateIds) {
        requireTemplateIds(templateIds);
        if (templateIds.size() >= ASYNC_CONFIRM_THRESHOLD) {
            Long taskId = recalculationTaskService.createTask(userId, "batch_confirm", templateIds.size());
            recalculationTaskService.executeBatchConfirm(taskId, templateIds);
            BatchConfirmVO vo = new BatchConfirmVO();
            vo.setMode("async");
            vo.setTaskId(taskId);
            return vo;
        }
        BatchConfirmVO vo = BatchConfirmVO.summarize(templateIds.stream()
                .map(id -> confirmSingleTemplate(id, userId))
                .collect(Collectors.toList()));
        vo.setMode("sync");
        return vo;
    }

    /**
     * R003 批量跳过编排：跳过轻量，恒同步逐笔执行
     */
    public BatchConfirmVO batchSkip(Long userId, List<Long> templateIds) {
        requireTemplateIds(templateIds);
        BatchConfirmVO vo = BatchConfirmVO.summarize(templateIds.stream()
                .map(id -> skipSingleTemplate(id, userId))
                .collect(Collectors.toList()));
        vo.setMode("sync");
        return vo;
    }

    private void requireTemplateIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "templateIds 不能为空");
        }
    }

    /**
     * R003 月度日历入口：month 解析（YYYY-MM）+ 范围校验（过去 12 个月 ~ 未来 12 个月）
     */
    public RecurringCalendarVO getCalendar(Long userId, String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month == null ? "" : month.trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "month 格式应为 YYYY-MM");
        }
        YearMonth current = YearMonth.now();
        if (ym.isBefore(current.minusMonths(12)) || ym.isAfter(current.plusMonths(12))) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "month 仅允许查询过去 12 个月至未来 12 个月");
        }
        return buildCalendar(userId, ym);
    }

    /**
     * R003 确认落账 DTO：date=nextTriggerDate（当期），mode=simple，带 sourceTemplateId 闭环
     */
    public TransactionDTO buildConfirmDTO(RecurringTemplate template) {
        TransactionDTO dto = new TransactionDTO();
        dto.setDate(template.getNextTriggerDate());
        dto.setTime(LocalTime.NOON);
        dto.setAmount(template.getAmount());
        dto.setDescription(buildConfirmDescription(template));
        dto.setTransactionType(template.getType());
        dto.setAccountId(template.getAccountId());
        if ("transfer".equals(template.getType())) {
            dto.setTargetAccountId(template.getToAccountId());
        }
        dto.setCategoryId(template.getCategoryId());
        dto.setMode("simple");
        dto.setSourceTemplateId(template.getId());
        return dto;
    }

    private String buildConfirmDescription(RecurringTemplate template) {
        String base = template.getDescription() == null || template.getDescription().isEmpty()
                ? template.getName() : template.getDescription();
        return template.getName() + " · " + base;
    }

    /**
     * R003 月度日历：推算模板在指定月的全部计划期。
     * 期次从 startDate 起连续编号：daily 逐日 / weekly 按 dayOfWeek / monthly 按 dayOfMonth（短月截断到月末），
     * endDate 与 maxCount 越界截断。R004 起由 plannedDatesInRange 泛化实现。
     */
    public List<LocalDate> plannedDatesInMonth(RecurringTemplate template, YearMonth month) {
        return plannedDatesInRange(template, month.atDay(1), month.atEndOfMonth());
    }

    /**
     * R004 计划窗口推算（plannedDatesInMonth 泛化）：模板在 [rangeStart, rangeEnd]（含端点）
     * 内的全部计划期；期次编号、endDate 与 maxCount 截断逻辑与月度日历一致。
     */
    public List<LocalDate> plannedDatesInRange(RecurringTemplate template,
                                               LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate start = template.getStartDate() != null ? template.getStartDate() : LocalDate.now();
        LocalDate end = template.getEndDate();
        int maxCount = template.getMaxCount() != null && template.getMaxCount() > 0
                ? template.getMaxCount() : Integer.MAX_VALUE;

        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = firstOccurrence(template, start);
        int period = 1;
        while (cursor != null && !cursor.isAfter(rangeEnd)) {
            if (period > maxCount) {
                break;
            }
            if (end != null && cursor.isAfter(end)) {
                break;
            }
            if (!cursor.isBefore(rangeStart)) {
                dates.add(cursor);
            }
            cursor = nextOccurrence(template, cursor);
            period++;
        }
        return dates;
    }

    /**
     * R003 月度日历视图：所有 active 模板按计划期平铺，status 三态
     * （date < nextTriggerDate → done；date <= 今天 → due；否则 planned）
     */
    public RecurringCalendarVO buildCalendar(Long userId, YearMonth month) {
        LocalDate today = LocalDate.now();
        RecurringCalendarVO vo = new RecurringCalendarVO();
        vo.setMonth(month.toString());
        for (RecurringTemplate template : recurringTemplateMapper.selectByUserId(userId)) {
            if (!"active".equals(template.getStatus())) {
                continue;
            }
            LocalDate next = template.getNextTriggerDate();
            for (LocalDate date : plannedDatesInMonth(template, month)) {
                RecurringCalendarVO.Item item = new RecurringCalendarVO.Item();
                item.setTemplateId(template.getId());
                item.setName(template.getName());
                item.setAmount(template.getAmount());
                item.setType(template.getType());
                item.setFrequency(template.getFrequency());
                item.setDate(date);
                item.setStatus(next != null && date.isBefore(next) ? "done"
                        : (!date.isAfter(today) ? "due" : "planned"));
                vo.getItems().add(item);
            }
        }
        vo.getItems().sort(Comparator.comparing(RecurringCalendarVO.Item::getDate)
                .thenComparing(RecurringCalendarVO.Item::getTemplateId));
        return vo;
    }

    // ===== R004 订阅管理视图 =====

    /**
     * 订阅总览：active 支出模板折算月额 + 涨价检测 + 近 30 天计划扣款窗口
     */
    public SubscriptionOverviewVO getSubscriptionOverview(Long userId) {
        Map<Long, BigDecimal> lastPaid = latestTemplateAmounts(userId);
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(SUBSCRIPTION_WINDOW_DAYS);

        SubscriptionOverviewVO vo = new SubscriptionOverviewVO();
        List<SubscriptionOverviewVO.Item> items = new ArrayList<>();
        BigDecimal monthlyTotal = BigDecimal.ZERO;
        BigDecimal windowAmount = BigDecimal.ZERO;
        int windowCount = 0;

        for (RecurringTemplate template : recurringTemplateMapper.selectByUserId(userId)) {
            if (!isSubscription(template)) {
                continue;
            }
            SubscriptionOverviewVO.Item item =
                    buildSubscriptionItem(template, lastPaid.get(template.getId()));
            items.add(item);
            monthlyTotal = monthlyTotal.add(item.getMonthlyAmount());
            for (LocalDate date : plannedDatesInRange(template, today, windowEnd)) {
                windowCount++;
                windowAmount = windowAmount.add(template.getAmount());
            }
        }

        items.sort(Comparator.comparing(SubscriptionOverviewVO.Item::getMonthlyAmount).reversed());
        vo.setItems(items);
        vo.setItemCount(items.size());
        vo.setMonthlyTotal(monthlyTotal);
        vo.setNext30DaysCount(windowCount);
        vo.setNext30DaysAmount(windowAmount);
        return vo;
    }

    /**
     * 订阅口径：status=active && type=expense && amount>0（不加字段）
     */
    private boolean isSubscription(RecurringTemplate template) {
        return template != null
                && "active".equals(template.getStatus())
                && "expense".equals(template.getType())
                && template.getAmount() != null
                && template.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 频率折算月额（HALF_UP 2 位）：
     * monthly=原额；weekly=×52÷12；daily=×365÷12；yearly=÷12
     */
    public BigDecimal convertToMonthly(String frequency, BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthly;
        switch (frequency == null ? "monthly" : frequency) {
            case "weekly":
                monthly = amount.multiply(BigDecimal.valueOf(52))
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                break;
            case "daily":
                monthly = amount.multiply(BigDecimal.valueOf(365))
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                break;
            case "yearly":
                monthly = amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                break;
            case "monthly":
            default:
                monthly = amount.setScale(2, RoundingMode.HALF_UP);
                break;
        }
        return monthly.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 组装单条订阅项：折算月额 + 涨价三态（当前金额 > 最近一笔模板交易金额）
     *
     * @param lastPaidAmount 该模板最近一笔交易金额（无基准时 null，恒不误报）
     */
    public SubscriptionOverviewVO.Item buildSubscriptionItem(RecurringTemplate template,
                                                             BigDecimal lastPaidAmount) {
        SubscriptionOverviewVO.Item item = new SubscriptionOverviewVO.Item();
        item.setTemplateId(template.getId());
        item.setName(template.getName());
        item.setAmount(template.getAmount().setScale(2, RoundingMode.HALF_UP));
        item.setFrequency(template.getFrequency());
        item.setMonthlyAmount(convertToMonthly(template.getFrequency(), template.getAmount()));
        item.setNextTriggerDate(template.getNextTriggerDate());
        item.setLastPaidAmount(lastPaidAmount);
        if (lastPaidAmount != null && template.getAmount().compareTo(lastPaidAmount) > 0) {
            item.setPriceIncreased(true);
            BigDecimal diff = template.getAmount().subtract(lastPaidAmount);
            item.setIncreaseAmount(diff.setScale(2, RoundingMode.HALF_UP));
            item.setIncreasePercent(diff.multiply(BigDecimal.valueOf(100))
                    .divide(lastPaidAmount, 1, RoundingMode.HALF_UP));
        }
        return item;
    }

    /**
     * 涨价基准：每模板最近一笔 expense 交易金额（date DESC, id DESC 取 1）
     */
    private Map<Long, BigDecimal> latestTemplateAmounts(Long userId) {
        Map<Long, BigDecimal> byTemplate = new HashMap<>();
        if (transactionMapper == null) {
            return byTemplate;
        }
        for (Map<String, Object> row : transactionMapper.selectLatestTemplateAmounts(userId)) {
            Object templateId = row.get("templateId");
            Object amount = row.get("amount");
            if (templateId instanceof Number && amount instanceof Number) {
                byTemplate.put(((Number) templateId).longValue(), new BigDecimal(amount.toString()));
            }
        }
        return byTemplate;
    }

    private boolean ownedBy(RecurringTemplate template, Long userId) {
        return template != null && userId != null && userId.equals(template.getUserId())
                && !Integer.valueOf(1).equals(template.getIsDeleted());
    }

    /**
     * 推进模板：期次连续（confirmed+1 期），maxCount 收口，消化当期待办
     */
    private void advanceTemplate(RecurringTemplate template) {
        LocalDate confirmedDate = template.getNextTriggerDate();
        int executedCount = template.getExecutedCount() + 1;
        LocalDate nextTriggerDate = nextOccurrence(template, confirmedDate);
        if (nextTriggerDate == null || !nextTriggerDate.isAfter(confirmedDate)) {
            nextTriggerDate = calculateNextTriggerDate(template);
        }

        recurringTemplateMapper.updateExecution(template.getId(), executedCount, nextTriggerDate, LocalDateTime.now());

        if (template.getMaxCount() != null && executedCount >= template.getMaxCount()) {
            recurringTemplateMapper.updateStatus(template.getId(), "completed");
        }

        notificationService.digestRecurringTodo(template.getUserId(), template.getId(), confirmedDate);
    }

    /** 第 1 期发生日 */
    private LocalDate firstOccurrence(RecurringTemplate template, LocalDate start) {
        String frequency = template.getFrequency() == null ? "daily" : template.getFrequency();
        switch (frequency) {
            case "weekly": {
                Integer dayOfWeek = template.getDayOfWeek();
                if (dayOfWeek == null) {
                    return start;
                }
                int offset = (dayOfWeek - start.getDayOfWeek().getValue() + 7) % 7;
                return start.plusDays(offset);
            }
            case "monthly": {
                Integer dayOfMonth = template.getDayOfMonth() != null
                        ? template.getDayOfMonth() : start.getDayOfMonth();
                YearMonth ym = YearMonth.from(start);
                return ym.atDay(Math.min(dayOfMonth, ym.lengthOfMonth()));
            }
            case "yearly": {
                Integer monthOfYear = template.getMonthOfYear();
                if (monthOfYear == null) {
                    return start;
                }
                int dayOfMonth = template.getDayOfMonth() != null ? template.getDayOfMonth() : 1;
                LocalDate candidate = yearlyDate(start.getYear(), monthOfYear, dayOfMonth);
                if (candidate.isBefore(start)) {
                    candidate = yearlyDate(start.getYear() + 1, monthOfYear, dayOfMonth);
                }
                return candidate;
            }
            case "daily":
            default:
                return start;
        }
    }

    /** 年付发生日：dayOfMonth 超过月天数时截断到月末（如 2 月 29 → 2 月 28） */
    private LocalDate yearlyDate(int year, int monthOfYear, int dayOfMonth) {
        YearMonth ym = YearMonth.of(year, Math.min(Math.max(monthOfYear, 1), 12));
        return ym.atDay(Math.min(Math.max(dayOfMonth, 1), ym.lengthOfMonth()));
    }

    /** 下一期发生日（期次连续；monthly 短月截断到月末） */
    private LocalDate nextOccurrence(RecurringTemplate template, LocalDate current) {
        if (current == null) {
            return null;
        }
        String frequency = template.getFrequency() == null ? "daily" : template.getFrequency();
        switch (frequency) {
            case "weekly":
                return current.plusWeeks(1);
            case "monthly": {
                Integer dayOfMonth = template.getDayOfMonth() != null
                        ? template.getDayOfMonth() : current.getDayOfMonth();
                YearMonth nextMonth = YearMonth.from(current).plusMonths(1);
                return nextMonth.atDay(Math.min(dayOfMonth, nextMonth.lengthOfMonth()));
            }
            case "yearly":
                return current.plusYears(1);
            case "daily":
            default:
                return current.plusDays(1);
        }
    }

    /**
     * 暂停模板
     *
     * @param id 模板ID
     */
    @Transactional
    public void pauseTemplate(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        recurringTemplateMapper.updateStatus(id, "paused");
    }

    /**
     * 恢复模板：触发日仍在今天或未来则原样保留（不压缩到明天），已过期则重排到下一未来周期
     *
     * @param id 模板ID
     */
    @Transactional
    public void resumeTemplate(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        LocalDate today = LocalDate.now();
        LocalDate next = template.getNextTriggerDate();
        if (next == null || next.isBefore(today)) {
            next = calculateNextTriggerDate(template);
        }
        template.setStatus("active");
        template.setNextTriggerDate(next);
        recurringTemplateMapper.updateById(template);
    }

    /**
     * 计算下次触发日期
     */
    private LocalDate calculateNextTriggerDate(RecurringTemplateDTO dto) {
        LocalDate baseDate = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();

        switch (dto.getFrequency()) {
            case "daily":
                return baseDate.plusDays(1);
            case "weekly":
                return baseDate.plusWeeks(1);
            case "monthly":
                return baseDate.plusMonths(1);
            case "yearly":
                return baseDate.plusYears(1);
            default:
                return baseDate;
        }
    }

    /**
     * 计算下次触发日期（用于已有模板）：
     * 触发日在未来 → 从该期顺延一个周期；已过期或为当日 → 以今天为基点重排。
     * 未来期被提前确认时不会压缩回「明天」，保证期次连续。
     */
    private LocalDate calculateNextTriggerDate(RecurringTemplate template) {
        LocalDate base = LocalDate.now();
        if (template.getNextTriggerDate() != null && template.getNextTriggerDate().isAfter(base)) {
            base = template.getNextTriggerDate();
        }

        switch (template.getFrequency()) {
            case "daily":
                return base.plusDays(1);
            case "weekly":
                return base.plusWeeks(1);
            case "monthly":
                return base.plusMonths(1);
            case "yearly":
                return base.plusYears(1);
            default:
                return base;
        }
    }

    /**
     * 转换为VO
     */
    private RecurringTemplateVO convertToVO(RecurringTemplate template) {
        RecurringTemplateVO vo = new RecurringTemplateVO();
        vo.setId(template.getId());
        vo.setName(template.getName());
        vo.setType(template.getType());
        vo.setAmount(template.getAmount());
        vo.setCategoryId(template.getCategoryId());
        vo.setAccountId(template.getAccountId());
        vo.setToAccountId(template.getToAccountId());
        vo.setDescription(template.getDescription());
        vo.setFrequency(template.getFrequency());
        vo.setDayOfWeek(template.getDayOfWeek());
        vo.setDayOfMonth(template.getDayOfMonth());
        vo.setMonthOfYear(template.getMonthOfYear());
        vo.setStartDate(template.getStartDate());
        vo.setEndDate(template.getEndDate());
        vo.setMaxCount(template.getMaxCount());
        vo.setExecutedCount(template.getExecutedCount());
        vo.setNextTriggerDate(template.getNextTriggerDate());
        vo.setLastTriggeredAt(template.getLastTriggeredAt());
        vo.setIsDeleted(template.getIsDeleted());
        vo.setStatus(template.getStatus());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }
}
