package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.*;
import com.duoduocode.service.budget.entity.BudgetCarryover;
import com.duoduocode.service.budget.entity.SpecialBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.SpecialBudgetMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 预算服务类 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final CategoryMapper categoryMapper;
    private final EntryMapper entryMapper;
    private final SpecialBudgetMapper specialBudgetMapper;
    private final BudgetCarryoverMapper budgetCarryoverMapper;

    // ===== 日常预算相关 =====

    /**
     * 获取日常预算
     *
     * @param userId 用户ID
     * @param month 月份 YYYY-MM
     * @return 日常预算列表
     */
    public List<DailyBudgetVO> getDailyBudget(Long userId, String month) {
        // 查询用户所有支出分类
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, "expense");

        List<DailyBudgetVO> result = new ArrayList<>();
        for (Category category : categories) {
            if (category.getMonthlyBudget() != null && category.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
                DailyBudgetVO vo = buildDailyBudgetVO(category, month);
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 设置日常预算
     *
     * @param userId 用户ID
     * @param dto 日常预算数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDailyBudget(Long userId, DailyBudgetDTO dto) {
        // 验证分类存在且属于当前用户
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作此分类");
        }

        // 更新分类的预算设置
        Category updateCategory = new Category();
        updateCategory.setId(dto.getCategoryId());
        if (dto.getMonthlyBudget() != null) {
            updateCategory.setMonthlyBudget(dto.getMonthlyBudget());
        }
        if (dto.getWeeklyBudget() != null) {
            updateCategory.setWeeklyBudget(dto.getWeeklyBudget());
        }
        if (dto.getAlertThreshold() != null) {
            updateCategory.setAlertThreshold(dto.getAlertThreshold());
        }

        categoryMapper.updateById(updateCategory);
    }

    /**
     * 获取预算使用情况
     *
     * @param userId 用户ID
     * @param month 月份 YYYY-MM
     * @return 预算使用情况列表
     */
    public List<DailyBudgetVO> getDailyBudgetUsage(Long userId, String month) {
        // 查询用户所有支出分类
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, "expense");

        List<DailyBudgetVO> result = new ArrayList<>();
        for (Category category : categories) {
            if (category.getMonthlyBudget() != null && category.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
                DailyBudgetVO vo = buildDailyBudgetVOWithUsage(category, month);
                result.add(vo);
            }
        }

        return result;
    }

    // ===== 专项预算相关 =====

    /**
     * 获取专项预算列表
     *
     * @param userId 用户ID
     * @return 专项预算列表
     */
    public List<SpecialBudgetVO> getSpecialBudgetList(Long userId) {
        List<SpecialBudget> budgets = specialBudgetMapper.selectByUserId(userId);

        List<SpecialBudgetVO> result = new ArrayList<>();
        for (SpecialBudget budget : budgets) {
            SpecialBudgetVO vo = convertToSpecialBudgetVO(budget);
            result.add(vo);
        }

        return result;
    }

    /**
     * 获取专项预算详情
     *
     * @param id 预算ID
     * @return 专项预算详情
     */
    public SpecialBudgetVO getSpecialBudgetDetail(Long id) {
        SpecialBudget budget = specialBudgetMapper.selectById(id);
        if (budget == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }

        SpecialBudgetVO vo = convertToSpecialBudgetVO(budget);

        // 计算已使用金�?       
        BigDecimal usedAmount = calculateSpecialBudgetUsedAmount(budget);
        vo.setUsedAmount(usedAmount);

        // 计算剩余金额
        BigDecimal remainingAmount = budget.getTotalAmount().subtract(usedAmount);
        vo.setRemainingAmount(remainingAmount);

        // 计算使用百分比
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usagePercent = usedAmount
                    .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setUsagePercent(usagePercent);
        } else {
            vo.setUsagePercent(BigDecimal.ZERO);
        }

        // 计算时间进度
        vo.setProgressPercent(calculateProgressPercent(budget));

        return vo;
    }

    /**
     * 创建专项预算
     *
     * @param userId 用户ID
     * @param dto 专项预算数据
     * @return 创建的预算ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createSpecialBudget(Long userId, SpecialBudgetDTO dto) {
        // 验证必填参数
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预算名称不能为空");
        }
        if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预算金额必须大于0");
        }
        if (dto.getStartDate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能为空");
        }
        if (dto.getEndDate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能为空");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }

        // 创建专项预算
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(userId);
        budget.setName(dto.getName().trim());
        budget.setTotalAmount(dto.getTotalAmount());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setStatus("ongoing");
        budget.setNote(dto.getNote());
        budget.setActualAmount(BigDecimal.ZERO);
        budget.setCreatedAt(LocalDateTime.now());

        specialBudgetMapper.insert(budget);
        return budget.getId();
    }

    /**
     * 更新专项预算
     *
     * @param id 预算ID
     * @param dto 专项预算数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialBudget(Long id, SpecialBudgetDTO dto) {
        SpecialBudget existing = specialBudgetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }
        if (!"ongoing".equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只能修改进行中的预算");
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
            }
        }

        // 更新预算
        SpecialBudget budget = new SpecialBudget();
        budget.setId(id);
        if (dto.getName() != null) budget.setName(dto.getName().trim());
        if (dto.getTotalAmount() != null) budget.setTotalAmount(dto.getTotalAmount());
        if (dto.getStartDate() != null) budget.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) budget.setEndDate(dto.getEndDate());
        if (dto.getNote() != null) budget.setNote(dto.getNote());

        specialBudgetMapper.updateById(budget);
    }

    /**
     * 结束专项预算
     *
     * @param id 预算ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeSpecialBudget(Long id) {
        SpecialBudget existing = specialBudgetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }
        if (!"ongoing".equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该预算已结束");
        }

        // 计算实际支出金额
        BigDecimal actualAmount = calculateSpecialBudgetUsedAmount(existing);

        // 更新状态为已完成
        specialBudgetMapper.updateStatusToCompleted(id, actualAmount);
    }

    /**
     * 获取可用专项预算
     *
     * @param userId 用户ID
     * @return 可用专项预算列表
     */
    public List<SpecialBudgetVO> getAvailableSpecialBudgets(Long userId) {
        List<SpecialBudget> budgets = specialBudgetMapper.selectOngoingByUserId(userId);

        List<SpecialBudgetVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (SpecialBudget budget : budgets) {
            // 只返回当前日期在预算期间内的预算
            if (!today.isBefore(budget.getStartDate()) && !today.isAfter(budget.getEndDate())) {
                SpecialBudgetVO vo = convertToSpecialBudgetVO(budget);
                result.add(vo);
            }
        }

        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 构建日常预算VO（不含使用情况）
     */
    private DailyBudgetVO buildDailyBudgetVO(Category category, String month) {
        DailyBudgetVO vo = new DailyBudgetVO();
        vo.setCategoryId(category.getId());
        vo.setCategoryName(category.getName());
        vo.setCategoryIcon(category.getIcon());
        vo.setCategoryColor(category.getColor());
        vo.setMonthlyBudget(category.getMonthlyBudget());
        vo.setWeeklyBudget(category.getWeeklyBudget());
        vo.setAlertThreshold(category.getAlertThreshold());

        // 获取结转金额
        BigDecimal carryoverAmount = budgetCarryoverMapper.sumCarryoverAmountByCategoryIdAndToMonth(
                category.getId(), month);
        vo.setCarryoverAmount(carryoverAmount != null ? carryoverAmount : BigDecimal.ZERO);

        // 计算可用预算
        BigDecimal availableBudget = category.getMonthlyBudget().add(vo.getCarryoverAmount());
        vo.setAvailableBudget(availableBudget);

        return vo;
    }

    /**
     * 构建日常预算VO（含使用情况�?     */
    private DailyBudgetVO buildDailyBudgetVOWithUsage(Category category, String month) {
        DailyBudgetVO vo = buildDailyBudgetVO(category, month);

        // 计算当月该分类的支出总额
        BigDecimal usedAmount = calculateCategoryExpense(category.getId(), month);
        vo.setUsedAmount(usedAmount);

        // 计算剩余金额
        BigDecimal remainingAmount = vo.getAvailableBudget().subtract(usedAmount);
        vo.setRemainingAmount(remainingAmount);

        // 计算使用百分比
        if (vo.getAvailableBudget().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal usagePercent = usedAmount
                    .divide(vo.getAvailableBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setUsagePercent(usagePercent);
        } else {
            vo.setUsagePercent(BigDecimal.ZERO);
        }

        // 判断预算状态
        String status = determineBudgetStatus(vo.getUsagePercent(), category.getAlertThreshold());
        vo.setStatus(status);

        return vo;
    }

    /**
     * 计算某分类某月的支出总额
     */
    private BigDecimal calculateCategoryExpense(Long categoryId, String month) {
        // 解析月份
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 通过Entry查询该分类的支出
        // 支出分类的支出金额记录在debit字段
        BigDecimal expense = entryMapper.sumDebitByCategoryIdAndDateRange(categoryId, startDate, endDate);

        return expense != null ? expense : BigDecimal.ZERO;
    }

    /**
     * 判断预算状�?     */
    private String determineBudgetStatus(BigDecimal usagePercent, BigDecimal alertThreshold) {
        if (usagePercent.compareTo(new BigDecimal("100")) >= 0) {
            return "exceeded"; // 超支
        } else if (alertThreshold != null && usagePercent.compareTo(alertThreshold) >= 0) {
            return "warning"; // 预警
        } else {
            return "normal"; // 正常
        }
    }

    /**
     * 转换专项预算为VO
     */
    private SpecialBudgetVO convertToSpecialBudgetVO(SpecialBudget budget) {
        SpecialBudgetVO vo = new SpecialBudgetVO();
        BeanUtils.copyProperties(budget, vo);
        return vo;
    }

    /**
     * 计算专项预算已使用金�?     */
    private BigDecimal calculateSpecialBudgetUsedAmount(SpecialBudget budget) {
        // 这里简化处理，实际应该根据专项预算关联的交易来计算
        // 可以通过扩展Entry表或创建关联表来实现
        // 目前返回actualAmount�?
        return budget.getActualAmount() != null ? budget.getActualAmount() : BigDecimal.ZERO;
    }

    /**
     * 计算预算时间进度百分�?     */
    private BigDecimal calculateProgressPercent(SpecialBudget budget) {
        LocalDate today = LocalDate.now();

        if (today.isBefore(budget.getStartDate())) {
            return BigDecimal.ZERO;
        }
        if (today.isAfter(budget.getEndDate())) {
            return new BigDecimal("100");
        }

        long totalDays = ChronoUnit.DAYS.between(budget.getStartDate(), budget.getEndDate()) + 1;
        long passedDays = ChronoUnit.DAYS.between(budget.getStartDate(), today) + 1;

        return new BigDecimal(passedDays)
                .divide(new BigDecimal(totalDays), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
