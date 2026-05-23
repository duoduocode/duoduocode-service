package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.CarryoverDTO;
import com.duoduocode.service.budget.dto.DailyBudgetDTO;
import com.duoduocode.service.budget.dto.DailyBudgetVO;
import com.duoduocode.service.budget.dto.SpecialBudgetDTO;
import com.duoduocode.service.budget.dto.SpecialBudgetVO;
import com.duoduocode.service.budget.entity.BudgetCarryover;
import com.duoduocode.service.budget.entity.SpecialBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.SpecialBudgetMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BudgetServiceIntegrationTest {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetCarryoverService budgetCarryoverService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SpecialBudgetMapper specialBudgetMapper;

    @Autowired
    private BudgetCarryoverMapper budgetCarryoverMapper;

    private Long testUserId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        User user = new User();
        user.setOpenid("test_openid_budget_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        // 创建测试分类（支出分类，带月度预算）
        Category category = new Category();
        category.setUserId(testUserId);
        category.setName("测试分类_" + System.currentTimeMillis());
        category.setType("expense");
        category.setMonthlyBudget(new BigDecimal("1000.00"));
        category.setAlertThreshold(new BigDecimal("0.8"));
        category.setSortOrder(0);
        category.setIcon("🍔");
        category.setCreatedAt(LocalDateTime.now());
        category.setIsDeleted(false);
        categoryMapper.insert(category);
        testCategoryId = category.getId();
    }

    // ===== BudgetService 测试 =====

    @Test
    void getDailyBudget_shouldReturnBudgetList() {
        List<DailyBudgetVO> budgets = budgetService.getDailyBudget(testUserId, "2026-05");

        assertNotNull(budgets);
        assertTrue(budgets.size() > 0);

        DailyBudgetVO budget = budgets.get(0);
        assertEquals(testCategoryId, budget.getCategoryId());
        assertEquals(new BigDecimal("1000.00"), budget.getMonthlyBudget());
    }

    @Test
    void setDailyBudget_shouldSuccess() {
        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(testCategoryId);
        dto.setMonthlyBudget(new BigDecimal("2000.00"));
        dto.setAlertThreshold(new BigDecimal("0.8"));

        assertDoesNotThrow(() -> {
            budgetService.setDailyBudget(testUserId, dto);
        });

        // 验证更新结果
        Category category = categoryMapper.selectById(testCategoryId);
        assertEquals(0, new BigDecimal("2000.00").compareTo(category.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("0.8").compareTo(category.getAlertThreshold()));
    }

    @Test
    void setDailyBudget_shouldThrowExceptionWhenCategoryNotExist() {
        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(99999L);
        dto.setMonthlyBudget(new BigDecimal("2000.00"));

        assertThrows(BusinessException.class, () -> {
            budgetService.setDailyBudget(testUserId, dto);
        });
    }

    @Test
    void createSpecialBudget_shouldSuccess() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("旅行预算");
        dto.setTotalAmount(new BigDecimal("5000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));
        dto.setNote("年度旅行");

        Long budgetId = budgetService.createSpecialBudget(testUserId, dto);

        assertNotNull(budgetId);

        // 验证创建结果
        SpecialBudget budget = specialBudgetMapper.selectById(budgetId);
        assertEquals("旅行预算", budget.getName());
        assertEquals(new BigDecimal("5000.00"), budget.getTotalAmount());
        assertEquals("ongoing", budget.getStatus());
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenEndDateBeforeStartDate() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("1000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().minusDays(1));

        assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
    }

    @Test
    void getSpecialBudgetDetail_shouldReturnDetail() {
        // 先创建专项预算
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("测试专项预算");
        budget.setTotalAmount(new BigDecimal("3000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(2));
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        // 获取详情
        SpecialBudgetVO vo = budgetService.getSpecialBudgetDetail(budget.getId());

        assertNotNull(vo);
        assertEquals("测试专项预算", vo.getName());
        assertEquals(new BigDecimal("3000.00"), vo.getTotalAmount());
    }

    @Test
    void completeSpecialBudget_shouldSuccess() {
        // 先创建专项预算
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("待完成预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now().minusMonths(1));
        budget.setEndDate(LocalDate.now());
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        // 完成预算
        assertDoesNotThrow(() -> {
            budgetService.completeSpecialBudget(budget.getId());
        });

        // 验证状态变更
        SpecialBudget updated = specialBudgetMapper.selectById(budget.getId());
        assertEquals("completed", updated.getStatus());
    }

    @Test
    void getDailyBudgetUsage_shouldReturnUsageList() {
        List<DailyBudgetVO> budgetUsages = budgetService.getDailyBudgetUsage(testUserId, "2026-05");

        assertNotNull(budgetUsages);
        assertTrue(budgetUsages.size() > 0);

        DailyBudgetVO budgetUsage = budgetUsages.get(0);
        assertEquals(testCategoryId, budgetUsage.getCategoryId());
        assertEquals(new BigDecimal("1000.00"), budgetUsage.getMonthlyBudget());
        assertNotNull(budgetUsage.getUsedAmount());
        assertNotNull(budgetUsage.getRemainingAmount());
        assertNotNull(budgetUsage.getUsagePercent());
        assertNotNull(budgetUsage.getStatus());
    }

    @Test
    void getSpecialBudgetList_shouldReturnList() {
        // 先创建两个专项预算
        SpecialBudget budget1 = new SpecialBudget();
        budget1.setUserId(testUserId);
        budget1.setName("测试专项预算1");
        budget1.setTotalAmount(new BigDecimal("3000.00"));
        budget1.setStartDate(LocalDate.now());
        budget1.setEndDate(LocalDate.now().plusMonths(2));
        budget1.setStatus("ongoing");
        budget1.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget1);

        SpecialBudget budget2 = new SpecialBudget();
        budget2.setUserId(testUserId);
        budget2.setName("测试专项预算2");
        budget2.setTotalAmount(new BigDecimal("5000.00"));
        budget2.setStartDate(LocalDate.now());
        budget2.setEndDate(LocalDate.now().plusMonths(3));
        budget2.setStatus("completed");
        budget2.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget2);

        // 获取列表
        List<SpecialBudgetVO> budgets = budgetService.getSpecialBudgetList(testUserId);

        assertNotNull(budgets);
        assertTrue(budgets.size() >= 2);
    }

    @Test
    void updateSpecialBudget_shouldSuccess() {
        // 先创建专项预算
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("待更新预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(1));
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        // 更新预算
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("已更新预算");
        dto.setTotalAmount(new BigDecimal("3000.00"));

        assertDoesNotThrow(() -> {
            budgetService.updateSpecialBudget(budget.getId(), dto);
        });

        // 验证更新结果
        SpecialBudget updated = specialBudgetMapper.selectById(budget.getId());
        assertEquals("已更新预算", updated.getName());
        assertEquals(new BigDecimal("3000.00"), updated.getTotalAmount());
    }

    @Test
    void updateSpecialBudget_shouldThrowExceptionWhenBudgetNotExist() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("1000.00"));

        assertThrows(BusinessException.class, () -> {
            budgetService.updateSpecialBudget(99999L, dto);
        });
    }

    @Test
    void updateSpecialBudget_shouldThrowExceptionWhenBudgetCompleted() {
        // 先创建已完成的专项预算
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("已完成预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now().minusMonths(1));
        budget.setEndDate(LocalDate.now());
        budget.setStatus("completed");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        // 尝试更新
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("试图更新");

        assertThrows(BusinessException.class, () -> {
            budgetService.updateSpecialBudget(budget.getId(), dto);
        });
    }

    @Test
    void getAvailableSpecialBudgets_shouldReturnAvailableList() {
        // 创建一个在当前日期范围内的预算
        SpecialBudget availableBudget = new SpecialBudget();
        availableBudget.setUserId(testUserId);
        availableBudget.setName("可用预算");
        availableBudget.setTotalAmount(new BigDecimal("3000.00"));
        availableBudget.setStartDate(LocalDate.now().minusDays(1));
        availableBudget.setEndDate(LocalDate.now().plusMonths(1));
        availableBudget.setStatus("ongoing");
        availableBudget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(availableBudget);

        // 创建一个已过期的预算
        SpecialBudget expiredBudget = new SpecialBudget();
        expiredBudget.setUserId(testUserId);
        expiredBudget.setName("已过期预算");
        expiredBudget.setTotalAmount(new BigDecimal("2000.00"));
        expiredBudget.setStartDate(LocalDate.now().minusMonths(2));
        expiredBudget.setEndDate(LocalDate.now().minusMonths(1));
        expiredBudget.setStatus("ongoing");
        expiredBudget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(expiredBudget);

        // 获取可用预算
        List<SpecialBudgetVO> available = budgetService.getAvailableSpecialBudgets(testUserId);

        assertNotNull(available);
        assertTrue(available.stream().anyMatch(b -> b.getName().equals("可用预算")));
        assertFalse(available.stream().anyMatch(b -> b.getName().equals("已过期预算")));
    }

    // ===== BudgetCarryoverService 测试 =====

    @Test
    void carryoverBudget_shouldSuccess() {
        CarryoverDTO dto = new CarryoverDTO();
        dto.setCategoryId(testCategoryId);
        dto.setFromMonth("2026-04");
        dto.setToMonth("2026-05");
        dto.setCarryoverAmount(new BigDecimal("500.00"));

        Long carryoverId = budgetCarryoverService.carryoverBudget(testUserId, dto);

        assertNotNull(carryoverId);

        // 验证结转记录
        BudgetCarryover carryover = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(testCategoryId, "2026-04");
        assertNotNull(carryover);
        assertEquals(testCategoryId, carryover.getCategoryId());
        assertEquals("2026-04", carryover.getFromMonth());
        assertEquals("2026-05", carryover.getToMonth());
        assertEquals(new BigDecimal("500.00"), carryover.getCarryoverAmount());
    }

    @Test
    void carryoverBudget_shouldThrowExceptionWhenCategoryNotExist() {
        CarryoverDTO dto = new CarryoverDTO();
        dto.setCategoryId(99999L);
        dto.setFromMonth("2026-04");
        dto.setToMonth("2026-05");
        dto.setCarryoverAmount(new BigDecimal("500.00"));

        assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.carryoverBudget(testUserId, dto);
        });
    }

    @Test
    void carryoverBudget_shouldThrowExceptionWhenAmountExceedAvailable() {
        // 设置月度预算为1000，尝试结转2000（超过可结转金额）
        CarryoverDTO dto = new CarryoverDTO();
        dto.setCategoryId(testCategoryId);
        dto.setFromMonth("2026-04");
        dto.setToMonth("2026-05");
        dto.setCarryoverAmount(new BigDecimal("2000.00")); // 超过月度预算1000

        assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.carryoverBudget(testUserId, dto);
        });
    }

    @Test
    void carryoverBudget_shouldThrowExceptionWhenDuplicate() {
        // 先创建一条结转记录
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("300.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

        // 尝试再次创建相同月份的结转记录
        CarryoverDTO dto = new CarryoverDTO();
        dto.setCategoryId(testCategoryId);
        dto.setFromMonth("2026-04");
        dto.setToMonth("2026-05");
        dto.setCarryoverAmount(new BigDecimal("500.00"));

        assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.carryoverBudget(testUserId, dto);
        });
    }

    @Test
    void calculateCarryoverAmount_shouldReturnCorrectAmount() {
        // 月度预算是1000，没有支出的情况下可结转金额应该是1000
        BigDecimal availableAmount = budgetCarryoverService.calculateCarryoverAmount(
                testUserId, testCategoryId, "2026-04");

        assertNotNull(availableAmount);
        assertEquals(new BigDecimal("1000.00"), availableAmount);
    }

    @Test
    void getCarryoverStatistics_shouldReturnStatistics() {
        // 先创建一条结转记录
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("500.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

        // 获取统计信息
        Map<String, Object> statistics = budgetCarryoverService.getCarryoverStatistics(testUserId, "2026-04");

        assertNotNull(statistics);
        assertEquals("2026-04", statistics.get("month"));
        assertEquals(1, statistics.get("carryoverCount"));
        assertEquals(new BigDecimal("500.00"), statistics.get("totalCarryoverAmount"));
    }

    @Test
    void getCarryoverHistory_shouldReturnHistory() {
        // 先创建一条结转记录
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("500.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

        // 获取结转历史
        List<Map<String, Object>> history = budgetCarryoverService.getCarryoverHistory(testUserId, "2026-04");

        assertNotNull(history);
        assertEquals(1, history.size());

        Map<String, Object> item = history.get(0);
        assertEquals(testCategoryId, item.get("categoryId"));
        assertEquals("2026-04", item.get("fromMonth"));
        assertEquals("2026-05", item.get("toMonth"));
    }

    // ===== 补充边界条件和异常场景测试 =====

    @Test
    void setDailyBudget_shouldThrowExceptionWhenPermissionDenied() {
        Long anotherUserId = createAnotherUser();

        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(testCategoryId);
        dto.setMonthlyBudget(new BigDecimal("2000.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.setDailyBudget(anotherUserId, dto);
        });
        assertEquals("无权操作此分类", exception.getMessage());
    }

    @Test
    void setDailyBudget_shouldUpdateAlertThresholdOnly() {
        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(testCategoryId);
        dto.setAlertThreshold(new BigDecimal("0.5"));

        budgetService.setDailyBudget(testUserId, dto);

        Category category = categoryMapper.selectById(testCategoryId);
        assertEquals(0, new BigDecimal("0.5").compareTo(category.getAlertThreshold()));
    }

    @Test
    void setDailyBudget_shouldUpdateWeeklyBudgetOnly() {
        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(testCategoryId);
        dto.setWeeklyBudget(new BigDecimal("200.00"));

        budgetService.setDailyBudget(testUserId, dto);

        Category category = categoryMapper.selectById(testCategoryId);
        assertEquals(0, new BigDecimal("200.00").compareTo(category.getWeeklyBudget()));
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenNameEmpty() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("");
        dto.setTotalAmount(new BigDecimal("5000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
        assertEquals("预算名称不能为空", exception.getMessage());
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenAmountZero() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(BigDecimal.ZERO);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
        assertEquals("预算金额必须大于0", exception.getMessage());
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenAmountNegative() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("-100.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
        assertEquals("预算金额必须大于0", exception.getMessage());
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenStartDateNull() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("5000.00"));
        dto.setStartDate(null);
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
        assertEquals("开始日期不能为空", exception.getMessage());
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenEndDateNull() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("5000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
        assertEquals("结束日期不能为空", exception.getMessage());
    }

    @Test
    void getSpecialBudgetDetail_shouldThrowExceptionWhenNotExist() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.getSpecialBudgetDetail(99999L);
        });
        assertEquals("专项预算不存在", exception.getMessage());
    }

    @Test
    void completeSpecialBudget_shouldThrowExceptionWhenNotExist() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.completeSpecialBudget(99999L);
        });
        assertEquals("专项预算不存在", exception.getMessage());
    }

    @Test
    void completeSpecialBudget_shouldThrowExceptionWhenAlreadyCompleted() {
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("已完成预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now().minusMonths(1));
        budget.setEndDate(LocalDate.now());
        budget.setStatus("completed");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.completeSpecialBudget(budget.getId());
        });
        assertEquals("该预算已结束", exception.getMessage());
    }

    @Test
    void updateSpecialBudget_shouldThrowExceptionWhenEndDateBeforeStartDate() {
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("测试预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(1));
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setStartDate(LocalDate.now().plusMonths(2));
        dto.setEndDate(LocalDate.now().plusMonths(1));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetService.updateSpecialBudget(budget.getId(), dto);
        });
        assertEquals("结束日期不能早于开始日期", exception.getMessage());
    }

    @Test
    void getDailyBudget_shouldReturnEmptyWhenNoExpenseCategories() {
        List<DailyBudgetVO> budgets = budgetService.getDailyBudget(testUserId, "2099-12");
        assertNotNull(budgets);
    }

    @Test
    void getDailyBudgetUsage_shouldReturnEmptyWhenNoExpenseCategories() {
        List<DailyBudgetVO> budgets = budgetService.getDailyBudgetUsage(testUserId, "2099-12");
        assertNotNull(budgets);
    }

    @Test
    void getSpecialBudgetList_shouldReturnEmptyWhenNoBudgets() {
        List<SpecialBudgetVO> budgets = budgetService.getSpecialBudgetList(testUserId);
        assertNotNull(budgets);
    }

    @Test
    void getAvailableSpecialBudgets_shouldReturnEmptyWhenNoBudgets() {
        List<SpecialBudgetVO> budgets = budgetService.getAvailableSpecialBudgets(testUserId);
        assertNotNull(budgets);
    }

    @Test
    void calculateCarryoverAmount_shouldThrowExceptionWhenCategoryNotExist() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.calculateCarryoverAmount(testUserId, 99999L, "2026-04");
        });
        assertEquals("分类不存在", exception.getMessage());
    }

    @Test
    void calculateCarryoverAmount_shouldThrowExceptionWhenPermissionDenied() {
        Long anotherUserId = createAnotherUser();

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.calculateCarryoverAmount(anotherUserId, testCategoryId, "2026-04");
        });
        assertEquals("无权操作此分类", exception.getMessage());
    }

    @Test
    void getCarryoverStatistics_shouldReturnEmptyWhenNoCarryovers() {
        Map<String, Object> statistics = budgetCarryoverService.getCarryoverStatistics(testUserId, "2099-12");
        assertNotNull(statistics);
        assertEquals("2099-12", statistics.get("month"));
        assertEquals(0, statistics.get("carryoverCount"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) statistics.get("totalCarryoverAmount")));
    }

    @Test
    void getCarryoverHistory_shouldReturnEmptyWhenNoCarryovers() {
        List<Map<String, Object>> history = budgetCarryoverService.getCarryoverHistory(testUserId, "2099-12");
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    private Long createAnotherUser() {
        User user = new User();
        user.setOpenid("another_user_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }
}