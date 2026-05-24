package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.*;
import com.duoduocode.service.budget.entity.BudgetCarryover;
import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.entity.SpecialBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final CategoryMapper categoryMapper;
    private final EntryMapper entryMapper;
    private final SpecialBudgetMapper specialBudgetMapper;
    private final BudgetCarryoverMapper budgetCarryoverMapper;
    private final DailyBudgetMapper dailyBudgetMapper;

    // ===== 日常预算相关 =====

    public List<DailyBudgetVO> getDailyBudget(Long userId, String month) {
        List<DailyBudget> budgets = dailyBudgetMapper.selectByUserIdAndMonth(userId, month);
        List<DailyBudgetVO> result = new ArrayList<>();
        for (DailyBudget db : budgets) {
            if (db.getMonthlyBudget() != null && db.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
                Category category = categoryMapper.selectById(db.getCategoryId());
                if (category != null) {
                    DailyBudgetVO vo = buildDailyBudgetVO(db, category, month);
                    result.add(vo);
                }
            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDailyBudget(Long userId, DailyBudgetDTO dto) {
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作此分类");
        }

        DailyBudget budget = new DailyBudget();
        budget.setUserId(userId);
        budget.setCategoryId(dto.getCategoryId());
        budget.setMonth(YearMonth.now().toString());
        budget.setMonthlyBudget(dto.getMonthlyBudget());
        budget.setWeeklyBudget(dto.getWeeklyBudget());
        budget.setAlertThreshold(dto.getAlertThreshold());

        dailyBudgetMapper.upsert(budget);
    }

    public List<DailyBudgetVO> getDailyBudgetUsage(Long userId, String month) {
        List<DailyBudget> budgets = dailyBudgetMapper.selectByUserIdAndMonth(userId, month);
        List<DailyBudgetVO> result = new ArrayList<>();
        for (DailyBudget db : budgets) {
            if (db.getMonthlyBudget() != null && db.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
                Category category = categoryMapper.selectById(db.getCategoryId());
                if (category != null) {
                    DailyBudgetVO vo = buildDailyBudgetVOWithUsage(db, category, month);
                    result.add(vo);
                }
            }
        }
        return result;
    }

    // ===== 专项预算相关 =====

    public List<SpecialBudgetVO> getSpecialBudgetList(Long userId) {
        List<SpecialBudget> budgets = specialBudgetMapper.selectByUserId(userId);
        List<SpecialBudgetVO> result = new ArrayList<>();
        for (SpecialBudget budget : budgets) {
            result.add(convertToSpecialBudgetVO(budget));
        }
        return result;
    }

    public SpecialBudgetVO getSpecialBudgetDetail(Long id) {
        SpecialBudget budget = specialBudgetMapper.selectById(id);
        if (budget == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }
        SpecialBudgetVO vo = convertToSpecialBudgetVO(budget);
        BigDecimal usedAmount = calculateSpecialBudgetUsedAmount(budget);
        vo.setUsedAmount(usedAmount);
        BigDecimal remainingAmount = budget.getTotalAmount().subtract(usedAmount);
        vo.setRemainingAmount(remainingAmount);
        if (budget.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            vo.setUsagePercent(usedAmount
                    .divide(budget.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            vo.setUsagePercent(BigDecimal.ZERO);
        }
        vo.setProgressPercent(calculateProgressPercent(budget));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createSpecialBudget(Long userId, SpecialBudgetDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预算名称不能为空");
        }
        if (dto.getTotalAmount() == null || dto.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预算金额必须大于0");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "日期不能为空");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }

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

    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialBudget(Long id, SpecialBudgetDTO dto) {
        SpecialBudget existing = specialBudgetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }
        if (!"ongoing".equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只能修改进行中的预算");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        SpecialBudget budget = new SpecialBudget();
        budget.setId(id);
        if (dto.getName() != null) budget.setName(dto.getName().trim());
        if (dto.getTotalAmount() != null) budget.setTotalAmount(dto.getTotalAmount());
        if (dto.getStartDate() != null) budget.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) budget.setEndDate(dto.getEndDate());
        if (dto.getNote() != null) budget.setNote(dto.getNote());
        specialBudgetMapper.updateById(budget);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeSpecialBudget(Long id) {
        SpecialBudget existing = specialBudgetMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "专项预算不存在");
        }
        if (!"ongoing".equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该预算已结束");
        }
        BigDecimal actualAmount = calculateSpecialBudgetUsedAmount(existing);
        specialBudgetMapper.updateStatusToCompleted(id, actualAmount);
    }

    public List<SpecialBudgetVO> getAvailableSpecialBudgets(Long userId) {
        List<SpecialBudget> budgets = specialBudgetMapper.selectOngoingByUserId(userId);
        List<SpecialBudgetVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (SpecialBudget budget : budgets) {
            if (!today.isBefore(budget.getStartDate()) && !today.isAfter(budget.getEndDate())) {
                result.add(convertToSpecialBudgetVO(budget));
            }
        }
        return result;
    }

    // ===== 私有辅助方法 =====

    private DailyBudgetVO buildDailyBudgetVO(DailyBudget db, Category category, String month) {
        DailyBudgetVO vo = new DailyBudgetVO();
        vo.setCategoryId(category.getId());
        vo.setCategoryName(category.getName());
        vo.setCategoryIcon(category.getIcon());
        vo.setCategoryColor(category.getColor());
        vo.setMonthlyBudget(db.getMonthlyBudget());
        vo.setWeeklyBudget(db.getWeeklyBudget());
        vo.setAlertThreshold(db.getAlertThreshold());

        BigDecimal carryoverAmount = budgetCarryoverMapper.sumCarryoverAmountByCategoryIdAndToMonth(
                category.getId(), month);
        vo.setCarryoverAmount(carryoverAmount != null ? carryoverAmount : BigDecimal.ZERO);

        BigDecimal availableBudget = db.getMonthlyBudget().add(vo.getCarryoverAmount());
        vo.setAvailableBudget(availableBudget);
        return vo;
    }

    private DailyBudgetVO buildDailyBudgetVOWithUsage(DailyBudget db, Category category, String month) {
        DailyBudgetVO vo = buildDailyBudgetVO(db, category, month);

        BigDecimal usedAmount = calculateCategoryExpense(category.getId(), month);
        vo.setUsedAmount(usedAmount);

        BigDecimal remainingAmount = vo.getAvailableBudget().subtract(usedAmount);
        vo.setRemainingAmount(remainingAmount);

        if (vo.getAvailableBudget().compareTo(BigDecimal.ZERO) > 0) {
            vo.setUsagePercent(usedAmount
                    .divide(vo.getAvailableBudget(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            vo.setUsagePercent(BigDecimal.ZERO);
        }

        vo.setStatus(determineBudgetStatus(vo.getUsagePercent(), db.getAlertThreshold()));
        return vo;
    }

    private BigDecimal calculateCategoryExpense(Long categoryId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        BigDecimal expense = entryMapper.sumDebitByCategoryIdAndDateRange(categoryId, startDate, endDate);
        return expense != null ? expense : BigDecimal.ZERO;
    }

    private String determineBudgetStatus(BigDecimal usagePercent, BigDecimal alertThreshold) {
        if (usagePercent.compareTo(new BigDecimal("100")) >= 0) {
            return "exceeded";
        } else if (alertThreshold != null && usagePercent.compareTo(alertThreshold) >= 0) {
            return "warning";
        } else {
            return "normal";
        }
    }

    private SpecialBudgetVO convertToSpecialBudgetVO(SpecialBudget budget) {
        SpecialBudgetVO vo = new SpecialBudgetVO();
        BeanUtils.copyProperties(budget, vo);
        return vo;
    }

    private BigDecimal calculateSpecialBudgetUsedAmount(SpecialBudget budget) {
        return budget.getActualAmount() != null ? budget.getActualAmount() : BigDecimal.ZERO;
    }

    private BigDecimal calculateProgressPercent(SpecialBudget budget) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(budget.getStartDate())) return BigDecimal.ZERO;
        if (today.isAfter(budget.getEndDate())) return new BigDecimal("100");
        long totalDays = ChronoUnit.DAYS.between(budget.getStartDate(), budget.getEndDate()) + 1;
        long passedDays = ChronoUnit.DAYS.between(budget.getStartDate(), today) + 1;
        return new BigDecimal(passedDays)
                .divide(new BigDecimal(totalDays), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
