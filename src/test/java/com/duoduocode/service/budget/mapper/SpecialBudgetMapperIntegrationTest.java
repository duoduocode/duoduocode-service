package com.duoduocode.service.budget.mapper;

import com.duoduocode.service.budget.entity.SpecialBudget;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SpecialBudgetMapperIntegrationTest {

    @Autowired
    private SpecialBudgetMapper specialBudgetMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_special_budget_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private SpecialBudget createSpecialBudget(String name, String status) {
        SpecialBudget budget = new SpecialBudget();
        budget.setUserId(testUserId);
        budget.setName(name);
        budget.setTotalAmount(new BigDecimal("10000.00"));
        budget.setActualAmount(BigDecimal.ZERO);
        budget.setStartDate(LocalDate.now());
        budget.setEndDate(LocalDate.now().plusMonths(3));
        budget.setStatus(status);
        budget.setNote("测试备注");
        budget.setCreatedAt(LocalDateTime.now());
        return budget;
    }

    @Test
    void insert_shouldSuccess() {
        SpecialBudget budget = createSpecialBudget("测试专项预算_" + System.currentTimeMillis(), "ongoing");

        int result = specialBudgetMapper.insert(budget);

        assertEquals(1, result);
        assertNotNull(budget.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        SpecialBudget budget = createSpecialBudget("完整字段测试_" + System.currentTimeMillis(), "ongoing");
        budget.setTotalAmount(new BigDecimal("50000.00"));
        budget.setStartDate(LocalDate.of(2026, 1, 1));
        budget.setEndDate(LocalDate.of(2026, 12, 31));

        specialBudgetMapper.insert(budget);

        SpecialBudget saved = specialBudgetMapper.selectById(budget.getId());
        assertNotNull(saved);
        assertEquals(testUserId, saved.getUserId());
        assertEquals(budget.getName(), saved.getName());
        assertEquals(0, new BigDecimal("50000.00").compareTo(saved.getTotalAmount()));
        assertEquals(LocalDate.of(2026, 1, 1), saved.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), saved.getEndDate());
        assertEquals("ongoing", saved.getStatus());
    }

    @Test
    void updateById_shouldSuccess() {
        SpecialBudget budget = createSpecialBudget("更新前_" + System.currentTimeMillis(), "ongoing");
        specialBudgetMapper.insert(budget);

        budget.setName("更新后");
        budget.setTotalAmount(new BigDecimal("20000.00"));
        budget.setNote("更新后的备注");

        int result = specialBudgetMapper.updateById(budget);

        assertEquals(1, result);

        SpecialBudget updated = specialBudgetMapper.selectById(budget.getId());
        assertEquals("更新后", updated.getName());
        assertEquals(0, new BigDecimal("20000.00").compareTo(updated.getTotalAmount()));
        assertEquals("更新后的备注", updated.getNote());
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        SpecialBudget budget = createSpecialBudget("不存在", "ongoing");
        budget.setId(99999L);

        int result = specialBudgetMapper.updateById(budget);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnBudget() {
        SpecialBudget budget = createSpecialBudget("查询测试_" + System.currentTimeMillis(), "ongoing");
        specialBudgetMapper.insert(budget);

        SpecialBudget result = specialBudgetMapper.selectById(budget.getId());

        assertNotNull(result);
        assertEquals(budget.getName(), result.getName());
        assertEquals(budget.getUserId(), result.getUserId());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        SpecialBudget result = specialBudgetMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByUserId_shouldReturnBudgetList() {
        specialBudgetMapper.insert(createSpecialBudget("预算1_" + System.currentTimeMillis(), "ongoing"));
        specialBudgetMapper.insert(createSpecialBudget("预算2_" + System.currentTimeMillis(), "ongoing"));
        specialBudgetMapper.insert(createSpecialBudget("预算3_" + System.currentTimeMillis(), "completed"));

        List<SpecialBudget> result = specialBudgetMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void selectByUserId_shouldReturnEmptyListWhenNoBudget() {
        List<SpecialBudget> result = specialBudgetMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectOngoingByUserId_shouldReturnOnlyOngoing() {
        specialBudgetMapper.insert(createSpecialBudget("进行中1_" + System.currentTimeMillis(), "ongoing"));
        specialBudgetMapper.insert(createSpecialBudget("进行中2_" + System.currentTimeMillis(), "ongoing"));
        specialBudgetMapper.insert(createSpecialBudget("已完成_" + System.currentTimeMillis(), "completed"));

        List<SpecialBudget> result = specialBudgetMapper.selectOngoingByUserId(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(budget -> assertEquals("ongoing", budget.getStatus()));
    }

    @Test
    void selectOngoingByUserId_shouldReturnEmptyListWhenNoOngoing() {
        specialBudgetMapper.insert(createSpecialBudget("已完成_" + System.currentTimeMillis(), "completed"));

        List<SpecialBudget> result = specialBudgetMapper.selectOngoingByUserId(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void updateStatusToCompleted_shouldSuccess() {
        SpecialBudget budget = createSpecialBudget("完成测试_" + System.currentTimeMillis(), "ongoing");
        specialBudgetMapper.insert(budget);

        int result = specialBudgetMapper.updateStatusToCompleted(budget.getId(), new BigDecimal("8500.00"));

        assertEquals(1, result);

        SpecialBudget completed = specialBudgetMapper.selectById(budget.getId());
        assertEquals("completed", completed.getStatus());
        assertEquals(0, new BigDecimal("8500.00").compareTo(completed.getActualAmount()));
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void updateStatusToCompleted_shouldReturnZeroWhenNotExist() {
        int result = specialBudgetMapper.updateStatusToCompleted(99999L, new BigDecimal("1000.00"));

        assertEquals(0, result);
    }

    @Test
    void deleteById_shouldSuccess() {
        SpecialBudget budget = createSpecialBudget("删除测试_" + System.currentTimeMillis(), "ongoing");
        specialBudgetMapper.insert(budget);
        Long budgetId = budget.getId();

        int result = specialBudgetMapper.deleteById(budgetId);

        assertEquals(1, result);

        SpecialBudget deleted = specialBudgetMapper.selectById(budgetId);
        assertNull(deleted);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = specialBudgetMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void multipleBudgets_shouldBeIndependent() {
        SpecialBudget budget1 = createSpecialBudget("预算A_" + System.currentTimeMillis(), "ongoing");
        budget1.setTotalAmount(new BigDecimal("10000.00"));
        specialBudgetMapper.insert(budget1);

        SpecialBudget budget2 = createSpecialBudget("预算B_" + System.currentTimeMillis(), "ongoing");
        budget2.setTotalAmount(new BigDecimal("20000.00"));
        specialBudgetMapper.insert(budget2);

        SpecialBudget result1 = specialBudgetMapper.selectById(budget1.getId());
        SpecialBudget result2 = specialBudgetMapper.selectById(budget2.getId());

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(0, new BigDecimal("10000.00").compareTo(result1.getTotalAmount()));
        assertEquals(0, new BigDecimal("20000.00").compareTo(result2.getTotalAmount()));
    }

    @Test
    void budgetLifecycle_shouldWorkCorrectly() {
        SpecialBudget budget = createSpecialBudget("生命周期测试_" + System.currentTimeMillis(), "ongoing");
        specialBudgetMapper.insert(budget);

        List<SpecialBudget> ongoingList = specialBudgetMapper.selectOngoingByUserId(testUserId);
        assertEquals(1, ongoingList.size());

        specialBudgetMapper.updateStatusToCompleted(budget.getId(), new BigDecimal("9500.00"));

        List<SpecialBudget> completedList = specialBudgetMapper.selectOngoingByUserId(testUserId);
        assertEquals(0, completedList.size());

        SpecialBudget completed = specialBudgetMapper.selectById(budget.getId());
        assertEquals("completed", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }
}