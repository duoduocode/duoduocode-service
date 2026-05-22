package com.duoduocode.service.recurring.service;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.recurring.dto.RecurringTemplateDTO;
import com.duoduocode.service.recurring.dto.RecurringTemplateVO;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 周期交易模板 Service
 */
@Service
@RequiredArgsConstructor
public class RecurringTemplateService {

    private final RecurringTemplateMapper recurringTemplateMapper;

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
        template.setDescription(dto.getDescription());
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
     * 手动触发提醒
     *
     * @param id 模板ID
     */
    @Transactional
    public void triggerTemplate(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 更新执行次数和下次触发日期
        int executedCount = template.getExecutedCount() + 1;
        LocalDate nextTriggerDate = calculateNextTriggerDate(template);

        recurringTemplateMapper.updateExecution(id, executedCount, nextTriggerDate, LocalDateTime.now());

        // 检查是否达到最大执行次数
        if (template.getMaxCount() != null && executedCount >= template.getMaxCount()) {
            recurringTemplateMapper.updateStatus(id, "completed");
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
     * 恢复模板
     *
     * @param id 模板ID
     */
    @Transactional
    public void resumeTemplate(Long id) {
        RecurringTemplate template = recurringTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 重新计算下次触发日期
        LocalDate nextTriggerDate = calculateNextTriggerDate(template);
        recurringTemplateMapper.updateStatus(id, "active");

        // 更新下次触发日期
        template.setNextTriggerDate(nextTriggerDate);
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
     * 计算下次触发日期（用于已有模板）
     */
    private LocalDate calculateNextTriggerDate(RecurringTemplate template) {
        LocalDate baseDate = LocalDate.now();

        switch (template.getFrequency()) {
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
        vo.setStatus(template.getStatus());
        vo.setCreatedAt(template.getCreatedAt());
        return vo;
    }
}
