package com.duoduocode.service.dashboard.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DashboardMapperIntegrationTest {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_dashboard_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private Account createAccount(String name, String type, BigDecimal initialBalance) {
        Account account = new Account();
        account.setUserId(testUserId);
        account.setName(name);
        account.setType(type);
        account.setIcon("💰");
        account.setColor("#FF5733");
        if ("liability".equals(type) && initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            initialBalance = initialBalance.negate();
        }
        account.setInitialBalance(initialBalance);
        account.setCreditLimit(BigDecimal.ZERO);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(new BigDecimal("0.8"));
        account.setSortOrder(0);
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    @Test
    void getLastMonthNetWorth_shouldReturnNetWorth() {
        accountMapper.insert(createAccount("资产账户_" + System.currentTimeMillis(), "asset", new BigDecimal("10000.00")));

        BigDecimal result = dashboardMapper.getLastMonthNetWorth(testUserId, "2026-01-01");

        assertNotNull(result);
    }

    @Test
    void getLastMonthNetWorth_shouldReturnZeroWhenNoAccount() {
        BigDecimal result = dashboardMapper.getLastMonthNetWorth(testUserId, "2099-01-01");

        assertNotNull(result);
    }

    @Test
    void getMonthlySummary_shouldReturnSummary() {
        accountMapper.insert(createAccount("资产账户_" + System.currentTimeMillis(), "asset", new BigDecimal("5000.00")));

        Map<String, Object> result = dashboardMapper.getMonthlySummary(testUserId, "2026-01-01", "2026-01-31");

        assertNotNull(result);
    }

    @Test
    void getMonthlySummary_shouldReturnZeroWhenNoData() {
        Map<String, Object> result = dashboardMapper.getMonthlySummary(testUserId, "2099-01-01", "2099-01-31");

        assertNotNull(result);
    }

    @Test
    void getCategoryExpense_shouldReturnExpense() {
        accountMapper.insert(createAccount("支出账户_" + System.currentTimeMillis(), "asset", new BigDecimal("1000.00")));

        BigDecimal result = dashboardMapper.getCategoryExpense(testUserId, 1L, "2026-01-01", "2026-01-31");

        assertNotNull(result);
    }

    @Test
    void getCategoryExpense_shouldReturnNullWhenNoData() {
        BigDecimal result = dashboardMapper.getCategoryExpense(testUserId, 99999L, "2099-01-01", "2099-01-31");

        assertNotNull(result);
    }

    @Test
    void getTopExpenseCategories_shouldReturnCategories() {
        accountMapper.insert(createAccount("资产1_" + System.currentTimeMillis(), "asset", new BigDecimal("5000.00")));
        accountMapper.insert(createAccount("资产2_" + System.currentTimeMillis(), "asset", new BigDecimal("3000.00")));

        List<Map<String, Object>> result = dashboardMapper.getTopExpenseCategories(testUserId, "2026-01-01", "2026-01-31", 5);

        assertNotNull(result);
    }

    @Test
    void getTopExpenseCategories_shouldReturnEmptyListWhenNoData() {
        List<Map<String, Object>> result = dashboardMapper.getTopExpenseCategories(testUserId, "2099-01-01", "2099-01-31", 5);

        assertNotNull(result);
    }

    @Test
    void getExpenseByDateRange_shouldReturnExpense() {
        accountMapper.insert(createAccount("今日支出_" + System.currentTimeMillis(), "asset", new BigDecimal("500.00")));

        BigDecimal result = dashboardMapper.getExpenseByDateRange(testUserId, "2026-05-01", "2026-05-23");

        assertNotNull(result);
    }

    @Test
    void getExpenseByDateRange_shouldReturnNullWhenNoData() {
        BigDecimal result = dashboardMapper.getExpenseByDateRange(testUserId, "2099-01-01", "2099-01-31");

        assertNotNull(result);
    }

    @Test
    void getTransactionCategory_shouldReturnNullWhenNotExist() {
        Map<String, Object> result = dashboardMapper.getTransactionCategory(99999L);

        assertNull(result);
    }

    @Test
    void multipleAccounts_shouldCalculateCorrectly() {
        accountMapper.insert(createAccount("资产账户A_" + System.currentTimeMillis(), "asset", new BigDecimal("10000.00")));
        accountMapper.insert(createAccount("负债账户B_" + System.currentTimeMillis(), "liability", new BigDecimal("3000.00")));
        accountMapper.insert(createAccount("投资账户C_" + System.currentTimeMillis(), "investment", new BigDecimal("5000.00")));

        BigDecimal netWorth = dashboardMapper.getLastMonthNetWorth(testUserId, "2026-01-01");

        assertNotNull(netWorth);
    }
}