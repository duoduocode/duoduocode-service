package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.BudgetCarryover;
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
class BudgetCarryoverMapperIntegrationTest {

    @Autowired
    private BudgetCarryoverMapper budgetCarryoverMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_carryover_" + System.currentTimeMillis());
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

    private BudgetCarryover createCarryover(String fromMonth, String toMonth, BigDecimal amount) {
        BudgetCarryover carryover = new BudgetCarryover();
        carryover.setCategoryId(testCategoryId);
        carryover.setFromMonth(fromMonth);
        carryover.setToMonth(toMonth);
        carryover.setCarryoverAmount(amount);
        carryover.setCreatedAt(LocalDateTime.now());
        return carryover;
    }

    @Test
    void insert_shouldSuccess() {
        BudgetCarryover carryover = createCarryover("2026-01", "2026-02", new BigDecimal("500.00"));

        int result = budgetCarryoverMapper.insert(carryover);

        assertEquals(1, result);
        assertNotNull(carryover.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        BudgetCarryover carryover = createCarryover("2026-03", "2026-04", new BigDecimal("300.00"));

        budgetCarryoverMapper.insert(carryover);

        BudgetCarryover saved = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-04");
        assertNotNull(saved);
        assertEquals(testCategoryId, saved.getCategoryId());
        assertEquals("2026-03", saved.getFromMonth());
        assertEquals("2026-04", saved.getToMonth());
        assertEquals(0, new BigDecimal("300.00").compareTo(saved.getCarryoverAmount()));
    }

    @Test
    void selectByCategoryIdAndToMonth_shouldReturnCarryover() {
        BudgetCarryover carryover = createCarryover("2026-05", "2026-06", new BigDecimal("200.00"));
        budgetCarryoverMapper.insert(carryover);

        BudgetCarryover result = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-06");

        assertNotNull(result);
        assertEquals(carryover.getId(), result.getId());
    }

    @Test
    void selectByCategoryIdAndToMonth_shouldReturnNullWhenNotExist() {
        BudgetCarryover result = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2099-12");

        assertNull(result);
    }

    @Test
    void selectByCategoryIdAndFromMonth_shouldReturnCarryover() {
        BudgetCarryover carryover = createCarryover("2026-07", "2026-08", new BigDecimal("150.00"));
        budgetCarryoverMapper.insert(carryover);

        BudgetCarryover result = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(testCategoryId, "2026-07");

        assertNotNull(result);
        assertEquals(carryover.getId(), result.getId());
    }

    @Test
    void selectByCategoryIdAndFromMonth_shouldReturnNullWhenNotExist() {
        BudgetCarryover result = budgetCarryoverMapper.selectByCategoryIdAndFromMonth(testCategoryId, "2099-01");

        assertNull(result);
    }

    @Test
    void selectByToMonth_shouldReturnList() {
        budgetCarryoverMapper.insert(createCarryover("2026-01", "2026-02", new BigDecimal("100.00")));
        budgetCarryoverMapper.insert(createCarryover("2026-01", "2026-03", new BigDecimal("200.00")));

        List<BudgetCarryover> result = budgetCarryoverMapper.selectByToMonth("2026-02");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void selectByToMonth_shouldReturnEmptyListWhenNoMatch() {
        List<BudgetCarryover> result = budgetCarryoverMapper.selectByToMonth("2099-12");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void sumCarryoverAmountByCategoryIdAndToMonth_shouldReturnSum() {
        budgetCarryoverMapper.insert(createCarryover("2026-01", "2026-02", new BigDecimal("100.00")));
        budgetCarryoverMapper.insert(createCarryover("2026-01", "2026-03", new BigDecimal("200.00")));

        BigDecimal sum = budgetCarryoverMapper.sumCarryoverAmountByCategoryIdAndToMonth(testCategoryId, "2026-02");

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("100.00").compareTo(sum));
    }

    @Test
    void sumCarryoverAmountByCategoryIdAndToMonth_shouldReturnZeroWhenNoMatch() {
        BigDecimal sum = budgetCarryoverMapper.sumCarryoverAmountByCategoryIdAndToMonth(testCategoryId, "2099-12");

        assertNotNull(sum);
        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }

    @Test
    void deleteById_shouldSuccess() {
        BudgetCarryover carryover = createCarryover("2026-09", "2026-10", new BigDecimal("50.00"));
        budgetCarryoverMapper.insert(carryover);
        Long carryoverId = carryover.getId();

        int result = budgetCarryoverMapper.deleteById(carryoverId);

        assertEquals(1, result);

        BudgetCarryover deleted = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-10");
        assertNull(deleted);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = budgetCarryoverMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void multipleCarryovers_shouldBeIndependent() {
        budgetCarryoverMapper.insert(createCarryover("2026-01", "2026-02", new BigDecimal("100.00")));
        budgetCarryoverMapper.insert(createCarryover("2026-02", "2026-03", new BigDecimal("200.00")));
        budgetCarryoverMapper.insert(createCarryover("2026-03", "2026-04", new BigDecimal("300.00")));

        BudgetCarryover feb = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-02");
        BudgetCarryover mar = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-03");
        BudgetCarryover apr = budgetCarryoverMapper.selectByCategoryIdAndToMonth(testCategoryId, "2026-04");

        assertNotNull(feb);
        assertNotNull(mar);
        assertNotNull(apr);
        assertEquals(0, new BigDecimal("100.00").compareTo(feb.getCarryoverAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(mar.getCarryoverAmount()));
        assertEquals(0, new BigDecimal("300.00").compareTo(apr.getCarryoverAmount()));
    }
}