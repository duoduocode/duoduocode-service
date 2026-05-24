package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.CarryoverDTO;
import com.duoduocode.service.budget.entity.BudgetCarryover;
import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetCarryoverService {

    private final BudgetCarryoverMapper budgetCarryoverMapper;
    private final CategoryMapper categoryMapper;
    private final EntryMapper entryMapper;
    private final DailyBudgetMapper dailyBudgetMapper;

    /**
     * 执行预算结转
     *
     * @param userId 用户ID
     * @param dto 结转数据
     * @return 创建的结转记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long carryoverBudget(Long userId, CarryoverDTO dto) {
        // 验证分类存在且属于当前用户
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作此分类");
        }

        // 验证月份格式
        if (dto.getFromMonth() == null || dto.getFromMonth().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "来源月份不能为空");
        }
        if (dto.getToMonth() == null || dto.getToMonth().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "目标月份不能为空");
        }

        // 验证结转金额
        if (dto.getCarryoverAmount() == null || dto.getCarryoverAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结转金额必须大于0");
        }

        // 计算可结转金额
        BigDecimal availableAmount = calculateCarryoverAmount(userId, dto.getCategoryId(), dto.getFromMonth());
        if (dto.getCarryoverAmount().compareTo(availableAmount) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "结转金额不能超过可结转金额" + availableAmount.toString());
        }

        // 检查是否已存在结转记录
        BudgetCarryover existing = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(
                dto.getCategoryId(), dto.getFromMonth());
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该月份已存在结转记录");
        }

        // 创建结转记录
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(dto.getCategoryId());
        carryover.setFromMonth(dto.getFromMonth());
        carryover.setToMonth(dto.getToMonth());
        carryover.setCarryoverAmount(dto.getCarryoverAmount());
        carryover.setCreatedAt(java.time.LocalDateTime.now());

        budgetCarryoverMapper.insert(carryover);
        return carryover.getId();
    }

    /**
     * 获取结转历史
     *
     * @param userId 用户ID
     * @param month 月份 YYYY-MM
     * @return 结转历史列表
     */
    public List<Map<String, Object>> getCarryoverHistory(Long userId, String month) {
        // 获取用户的所有支出分类
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, "expense");

        List<Map<String, Object>> result = new ArrayList<>();

        for (Category category : categories) {
            // 查询该分类的结转记录
            BudgetCarryover carryover = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(
                    category.getId(), month);

            if (carryover != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", carryover.getId());
                item.put("categoryId", carryover.getCategoryId());
                item.put("categoryName", category.getName());
                item.put("categoryIcon", category.getIcon());
                item.put("fromMonth", carryover.getFromMonth());
                item.put("toMonth", carryover.getToMonth());
                item.put("carryoverAmount", carryover.getCarryoverAmount());
                item.put("createdAt", carryover.getCreatedAt());
                result.add(item);
            }
        }

        return result;
    }

    /**
     * 计算可结转金额
     *
     * @param userId 用户ID
     * @param categoryId 分类ID
     * @param fromMonth 来源月份 YYYY-MM
     * @return 可结转金额
     */
    public BigDecimal calculateCarryoverAmount(Long userId, Long categoryId, String fromMonth) {
        // 验证分类存在且属于当前用户
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作此分类");
        }

        // 从budget_daily表获取该分类该月的月度预算
        DailyBudget dailyBudget = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(userId, categoryId, fromMonth);
        BigDecimal monthlyBudget = dailyBudget != null ? dailyBudget.getMonthlyBudget() : null;
        if (monthlyBudget == null || monthlyBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 解析月份
        YearMonth yearMonth = YearMonth.parse(fromMonth);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 计算当月该分类的支出总额
        BigDecimal usedAmount = entryMapper.sumDebitByCategoryIdAndDateRange(categoryId, startDate, endDate);
        if (usedAmount == null) {
            usedAmount = BigDecimal.ZERO;
        }

        // 计算剩余预算
        BigDecimal remainingBudget = monthlyBudget.subtract(usedAmount);

        // 可结转金额 = 剩余预算（如果为正数）
        return remainingBudget.compareTo(BigDecimal.ZERO) > 0 ? remainingBudget : BigDecimal.ZERO;
    }

    /**
     * 获取结转统计信息
     *
     * @param userId 用户ID
     * @param month 月份 YYYY-MM
     * @return 结转统计
     */
    public Map<String, Object> getCarryoverStatistics(Long userId, String month) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户的所有支出分类
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, "expense");

        BigDecimal totalCarryoverAmount = BigDecimal.ZERO;
        int carryoverCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Category category : categories) {
            // 查询该分类的结转记录
            BudgetCarryover carryover = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(
                    category.getId(), month);

            // 计算可结转金额
            DailyBudget dailyBudget = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(userId, category.getId(), month);
            BigDecimal monthlyBudget = dailyBudget != null ? dailyBudget.getMonthlyBudget() : null;
            BigDecimal availableAmount = calculateCarryoverAmount(userId, category.getId(), month);

            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", category.getId());
            item.put("categoryName", category.getName());
            item.put("categoryIcon", category.getIcon());
            item.put("monthlyBudget", monthlyBudget);
            item.put("availableAmount", availableAmount);

            if (carryover != null) {
                item.put("hasCarryover", true);
                item.put("carryoverAmount", carryover.getCarryoverAmount());
                item.put("toMonth", carryover.getToMonth());
                item.put("carryoverId", carryover.getId());
                totalCarryoverAmount = totalCarryoverAmount.add(carryover.getCarryoverAmount());
                carryoverCount++;
            } else {
                item.put("hasCarryover", false);
                item.put("carryoverAmount", BigDecimal.ZERO);
            }

            details.add(item);
        }

        result.put("month", month);
        result.put("totalCarryoverAmount", totalCarryoverAmount);
        result.put("carryoverCount", carryoverCount);
        result.put("categoryCount", categories.size());
        result.put("details", details);

        return result;
    }
}
