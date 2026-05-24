package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.CarryoverDTO;
import com.duoduocode.service.budget.dto.DailyBudgetDTO;
import com.duoduocode.service.budget.dto.DailyBudgetVO;
import com.duoduocode.service.budget.dto.SpecialBudgetDTO;
import com.duoduocode.service.budget.dto.SpecialBudgetVO;
import com.duoduocode.service.budget.entity.BudgetCarryover;
import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.entity.SpecialBudget;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
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

    @Autowired
    private DailyBudgetMapper dailyBudgetMapper;

    private Long testUserId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_budget_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        Category category = new Category();
        category.setUserId(testUserId);
        category.setName("测试分类_" + System.currentTimeMillis());
        category.setType("expense");
        category.setSortOrder(0);
        category.setIcon("🍔");
        category.setCreatedAt(LocalDateTime.now());
        category.setIsDeleted(false);
        categoryMapper.insert(category);
        testCategoryId = category.getId();

        DailyBudget dailyBudget = new DailyBudget();
        dailyBudget.setUserId(testUserId);
        dailyBudget.setCategoryId(testCategoryId);
        dailyBudget.setMonth("2026-05");
        dailyBudget.setMonthlyBudget(new BigDecimal("1000.00"));
        dailyBudget.setAlertThreshold(new BigDecimal("0.8"));
        dailyBudgetMapper.insert(dailyBudget);

        DailyBudget aprilBudget = new DailyBudget();
        aprilBudget.setUserId(testUserId);
        aprilBudget.setCategoryId(testCategoryId);
        aprilBudget.setMonth("2026-04");
        aprilBudget.setMonthlyBudget(new BigDecimal("1000.00"));
        dailyBudgetMapper.insert(aprilBudget);
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

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, java.time.YearMonth.now().toString());
        assertNotNull(saved);
        assertEquals(0, new BigDecimal("2000.00").compareTo(saved.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("0.8").compareTo(saved.getAlertThreshold()));
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
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("测试专项预算");
        budget.setTotalAmount(new BigDecimal("3000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(2));
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        SpecialBudgetVO vo = budgetService.getSpecialBudgetDetail(budget.getId());

        assertNotNull(vo);
        assertEquals("测试专项预算", vo.getName());
        assertEquals(new BigDecimal("3000.00"), vo.getTotalAmount());
    }

    @Test
    void completeSpecialBudget_shouldSuccess() {
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("待完成预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now().minusMonths(1));
        budget.setEndDate(LocalDate.now());
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        assertDoesNotThrow(() -> {
            budgetService.completeSpecialBudget(budget.getId());
        });

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

        List<SpecialBudgetVO> budgets = budgetService.getSpecialBudgetList(testUserId);

        assertNotNull(budgets);
        assertTrue(budgets.size() >= 2);
    }

    @Test
    void updateSpecialBudget_shouldSuccess() {
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("待更新预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(1));
        budget.setStatus("ongoing");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("已更新预算");
        dto.setTotalAmount(new BigDecimal("3000.00"));

        assertDoesNotThrow(() -> {
            budgetService.updateSpecialBudget(budget.getId(), dto);
        });

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
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName("已完成预算");
        budget.setTotalAmount(new BigDecimal("2000.00"));
        budget.setStartDate(LocalDate.now().minusMonths(1));
        budget.setEndDate(LocalDate.now());
        budget.setStatus("completed");
        budget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(budget);

        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("试图更新");

        assertThrows(BusinessException.class, () -> {
            budgetService.updateSpecialBudget(budget.getId(), dto);
        });
    }

    @Test
    void getAvailableSpecialBudgets_shouldReturnAvailableList() {
        SpecialBudget availableBudget = new SpecialBudget();
        availableBudget.setUserId(testUserId);
        availableBudget.setName("可用预算");
        availableBudget.setTotalAmount(new BigDecimal("3000.00"));
        availableBudget.setStartDate(LocalDate.now().minusDays(1));
        availableBudget.setEndDate(LocalDate.now().plusMonths(1));
        availableBudget.setStatus("ongoing");
        availableBudget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(availableBudget);

        SpecialBudget expiredBudget = new SpecialBudget();
        expiredBudget.setUserId(testUserId);
        expiredBudget.setName("已过期预算");
        expiredBudget.setTotalAmount(new BigDecimal("2000.00"));
        expiredBudget.setStartDate(LocalDate.now().minusMonths(2));
        expiredBudget.setEndDate(LocalDate.now().minusMonths(1));
        expiredBudget.setStatus("ongoing");
        expiredBudget.setCreatedAt(LocalDateTime.now());
        specialBudgetMapper.insert(expiredBudget);

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
        CarryoverDTO dto = new CarryoverDTO();
        dto.setCategoryId(testCategoryId);
        dto.setFromMonth("2026-04");
        dto.setToMonth("2026-05");
        dto.setCarryoverAmount(new BigDecimal("2000.00"));

        assertThrows(BusinessException.class, () -> {
            budgetCarryoverService.carryoverBudget(testUserId, dto);
        });
    }

    @Test
    void carryoverBudget_shouldThrowExceptionWhenDuplicate() {
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("300.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

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
        BigDecimal availableAmount = budgetCarryoverService.calculateCarryoverAmount(
                testUserId, testCategoryId, "2026-04");

        assertNotNull(availableAmount);
        assertEquals(new BigDecimal("1000.00"), availableAmount);
    }

    @Test
    void getCarryoverStatistics_shouldReturnStatistics() {
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("500.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

        Map<String, Object> statistics = budgetCarryoverService.getCarryoverStatistics(testUserId, "2026-04");

        assertNotNull(statistics);
        assertEquals("2026-04", statistics.get("month"));
        assertEquals(1, statistics.get("carryoverCount"));
        assertEquals(new BigDecimal("500.00"), statistics.get("totalCarryoverAmount"));
    }

    @Test
    void getCarryoverHistory_shouldReturnHistory() {
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth("2026-04");
        carryover.setToMonth("2026-05");
        carryover.setCarryoverAmount(new BigDecimal("500.00"));
        carryover.setCreatedAt(LocalDateTime.now());
        budgetCarryoverMapper.insert(carryover);

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

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, java.time.YearMonth.now().toString());
        assertNotNull(saved);
        assertEquals(0, new BigDecimal("0.5").compareTo(saved.getAlertThreshold()));
    }

    @Test
    void setDailyBudget_shouldUpdateWeeklyBudgetOnly() {
        DailyBudgetDTO dto = new DailyBudgetDTO();
        dto.setCategoryId(testCategoryId);
        dto.setWeeklyBudget(new BigDecimal("200.00"));

        budgetService.setDailyBudget(testUserId, dto);

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, java.time.YearMonth.now().toString());
        assertNotNull(saved);
        assertEquals(0, new BigDecimal("200.00").compareTo(saved.getWeeklyBudget()));
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

        assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
    }

    @Test
    void createSpecialBudget_shouldThrowExceptionWhenDateNull() {
        SpecialBudgetDTO dto = new SpecialBudgetDTO();
        dto.setName("测试预算");
        dto.setTotalAmount(new BigDecimal("1000.00"));

        assertThrows(BusinessException.class, () -> {
            budgetService.createSpecialBudget(testUserId, dto);
        });
    }

    @Test
    void getSpecialBudgetDetail_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            budgetService.getSpecialBudgetDetail(99999L);
        });
    }

    @Test
    void completeSpecialBudget_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            budgetService.completeSpecialBudget(99999L);
        });
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

        assertThrows(BusinessException.class, () -> {
            budgetService.completeSpecialBudget(budget.getId());
        });
    }

    private Long createAnotherUser() {
        User user = new User();
        user.setOpenid("test_openid_another_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }
}
