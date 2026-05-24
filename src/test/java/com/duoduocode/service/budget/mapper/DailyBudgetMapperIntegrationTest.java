package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DailyBudgetMapperIntegrationTest {

    @Autowired
    private DailyBudgetMapper dailyBudgetMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private Long testUserId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_daily_budget_" + System.currentTimeMillis());
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
        category.setIcon("🍔");
        category.setSortOrder(0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        testCategoryId = category.getId();
    }

    private DailyBudget createDailyBudget(String month) {
        DailyBudget budget = new DailyBudget();
        budget.setUserId(testUserId);
        budget.setCategoryId(testCategoryId);
        budget.setMonth(month);
        budget.setMonthlyBudget(new BigDecimal("1000.00"));
        budget.setWeeklyBudget(new BigDecimal("250.00"));
        budget.setAlertThreshold(new BigDecimal("0.80"));
        return budget;
    }

    @Test
    void insert_shouldSuccess() {
        DailyBudget budget = createDailyBudget("2026-05");

        int result = dailyBudgetMapper.insert(budget);

        assertEquals(1, result);
        assertNotNull(budget.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        DailyBudget budget = createDailyBudget("2026-06");
        budget.setWeeklyBudget(new BigDecimal("300.00"));
        budget.setAlertThreshold(new BigDecimal("0.90"));

        dailyBudgetMapper.insert(budget);

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-06");
        assertNotNull(saved);
        assertEquals(testUserId, saved.getUserId());
        assertEquals(testCategoryId, saved.getCategoryId());
        assertEquals("2026-06", saved.getMonth());
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("300.00").compareTo(saved.getWeeklyBudget()));
        assertEquals(0, new BigDecimal("0.90").compareTo(saved.getAlertThreshold()));
    }

    @Test
    void insert_shouldThrowExceptionWhenDuplicate() {
        DailyBudget budget = createDailyBudget("2026-05");
        dailyBudgetMapper.insert(budget);

        DailyBudget duplicate = createDailyBudget("2026-05");
        assertThrows(Exception.class, () -> dailyBudgetMapper.insert(duplicate));
    }

    @Test
    void updateById_shouldSuccess() {
        DailyBudget budget = createDailyBudget("2026-05");
        dailyBudgetMapper.insert(budget);

        budget.setMonthlyBudget(new BigDecimal("2000.00"));
        budget.setWeeklyBudget(new BigDecimal("500.00"));
        budget.setAlertThreshold(new BigDecimal("0.50"));

        int result = dailyBudgetMapper.updateById(budget);

        assertEquals(1, result);

        DailyBudget updated = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-05");
        assertEquals(0, new BigDecimal("2000.00").compareTo(updated.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("500.00").compareTo(updated.getWeeklyBudget()));
        assertEquals(0, new BigDecimal("0.50").compareTo(updated.getAlertThreshold()));
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        DailyBudget budget = createDailyBudget("2026-99");
        budget.setId(99999L);

        int result = dailyBudgetMapper.updateById(budget);

        assertEquals(0, result);
    }

    @Test
    void updateById_shouldUpdatePartialFields() {
        DailyBudget budget = createDailyBudget("2026-05");
        budget.setWeeklyBudget(null);
        budget.setAlertThreshold(null);
        dailyBudgetMapper.insert(budget);

        budget.setMonthlyBudget(new BigDecimal("3000.00"));

        dailyBudgetMapper.updateById(budget);

        DailyBudget updated = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-05");
        assertEquals(0, new BigDecimal("3000.00").compareTo(updated.getMonthlyBudget()));
    }

    @Test
    void upsert_shouldInsertWhenNotExist() {
        DailyBudget budget = createDailyBudget("2026-07");

        int result = dailyBudgetMapper.upsert(budget);

        assertEquals(1, result);

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-07");
        assertNotNull(saved);
        assertEquals("2026-07", saved.getMonth());
        assertEquals(0, new BigDecimal("1000.00").compareTo(saved.getMonthlyBudget()));
    }

    @Test
    void upsert_shouldUpdateWhenExist() {
        DailyBudget budget = createDailyBudget("2026-08");
        dailyBudgetMapper.insert(budget);

        DailyBudget upsertBudget = createDailyBudget("2026-08");
        upsertBudget.setMonthlyBudget(new BigDecimal("5000.00"));
        upsertBudget.setWeeklyBudget(new BigDecimal("1200.00"));
        upsertBudget.setAlertThreshold(new BigDecimal("0.60"));

        int result = dailyBudgetMapper.upsert(upsertBudget);

        assertTrue(result >= 1);

        DailyBudget updated = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-08");
        assertEquals(0, new BigDecimal("5000.00").compareTo(updated.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("1200.00").compareTo(updated.getWeeklyBudget()));
        assertEquals(0, new BigDecimal("0.60").compareTo(updated.getAlertThreshold()));
    }

    @Test
    void selectByUserIdAndMonth_shouldReturnFilteredList() {
        DailyBudget budget1 = createDailyBudget("2026-01");
        dailyBudgetMapper.insert(budget1);

        Category category2 = new Category();
        category2.setUserId(testUserId);
        category2.setName("过滤测试分类_" + System.currentTimeMillis());
        category2.setType("expense");
        category2.setIcon("🍕");
        category2.setSortOrder(1);
        category2.setIsDeleted(false);
        category2.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category2);

        DailyBudget budget2 = new DailyBudget();
        budget2.setUserId(testUserId);
        budget2.setCategoryId(category2.getId());
        budget2.setMonth("2026-02");
        budget2.setMonthlyBudget(new BigDecimal("500.00"));
        budget2.setWeeklyBudget(new BigDecimal("100.00"));
        dailyBudgetMapper.insert(budget2);

        List<DailyBudget> result = dailyBudgetMapper.selectByUserIdAndMonth(testUserId, "2026-02");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-02", result.get(0).getMonth());
    }

    @Test
    void selectByUserIdAndMonth_shouldReturnAllWhenMonthIsNull() {
        dailyBudgetMapper.insert(createDailyBudget("2026-01"));
        dailyBudgetMapper.insert(createDailyBudget("2026-02"));
        dailyBudgetMapper.insert(createDailyBudget("2026-03"));

        List<DailyBudget> result = dailyBudgetMapper.selectByUserIdAndMonth(testUserId, null);

        assertNotNull(result);
        assertTrue(result.size() >= 3);
    }

    @Test
    void selectByUserIdAndMonth_shouldReturnAllWhenMonthIsEmpty() {
        dailyBudgetMapper.insert(createDailyBudget("2026-01"));
        dailyBudgetMapper.insert(createDailyBudget("2026-02"));

        List<DailyBudget> result = dailyBudgetMapper.selectByUserIdAndMonth(testUserId, "");

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void selectByUserIdAndMonth_shouldReturnEmptyWhenNoBudget() {
        List<DailyBudget> result = dailyBudgetMapper.selectByUserIdAndMonth(testUserId, "2099-12");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByUserIdAndCategoryIdAndMonth_shouldReturnBudget() {
        DailyBudget budget = createDailyBudget("2026-09");
        dailyBudgetMapper.insert(budget);

        DailyBudget result = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-09");

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(testCategoryId, result.getCategoryId());
        assertEquals("2026-09", result.getMonth());
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.getMonthlyBudget()));
    }

    @Test
    void selectByUserIdAndCategoryIdAndMonth_shouldReturnNullWhenNotExist() {
        DailyBudget result = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, 99999L, "2099-12");

        assertNull(result);
    }

    @Test
    void selectByUserIdAndCategoryIdAndMonth_shouldReturnNullWhenMonthNotMatch() {
        DailyBudget budget = createDailyBudget("2026-10");
        dailyBudgetMapper.insert(budget);

        DailyBudget result = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-11");

        assertNull(result);
    }

    @Test
    void dailyBudgetLifecycle_shouldWorkCorrectly() {
        DailyBudget budget = createDailyBudget("2026-12");
        dailyBudgetMapper.insert(budget);
        assertNotNull(budget.getId());

        DailyBudget saved = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-12");
        assertNotNull(saved);

        saved.setMonthlyBudget(new BigDecimal("8000.00"));
        int updateResult = dailyBudgetMapper.updateById(saved);
        assertEquals(1, updateResult);

        DailyBudget updated = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-12");
        assertEquals(0, new BigDecimal("8000.00").compareTo(updated.getMonthlyBudget()));
    }

    @Test
    void multipleBudgets_shouldBeIndependent() {
        DailyBudget budget1 = createDailyBudget("2026-01");
        dailyBudgetMapper.insert(budget1);

        Category category2 = new Category();
        category2.setUserId(testUserId);
        category2.setName("独立分类_" + System.currentTimeMillis());
        category2.setType("expense");
        category2.setIcon("🍕");
        category2.setSortOrder(1);
        category2.setIsDeleted(false);
        category2.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category2);

        DailyBudget budget2 = new DailyBudget();
        budget2.setUserId(testUserId);
        budget2.setCategoryId(category2.getId());
        budget2.setMonth("2026-01");
        budget2.setMonthlyBudget(new BigDecimal("2000.00"));
        budget2.setWeeklyBudget(new BigDecimal("500.00"));
        dailyBudgetMapper.insert(budget2);

        DailyBudget result1 = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, testCategoryId, "2026-01");
        DailyBudget result2 = dailyBudgetMapper.selectByUserIdAndCategoryIdAndMonth(
                testUserId, category2.getId(), "2026-01");

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getCategoryId(), result2.getCategoryId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(result1.getMonthlyBudget()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(result2.getMonthlyBudget()));
    }
}
