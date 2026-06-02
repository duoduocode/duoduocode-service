package com.duoduocode.service.dashboard.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.dashboard.dto.DashboardVO;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class DashboardServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_dashboard_service_" + System.currentTimeMillis());
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
    void getDashboard_shouldReturnDashboard() {
        accountMapper.insert(createAccount("资产账户_" + System.currentTimeMillis(), "asset", new BigDecimal("10000.00")));
        accountMapper.insert(createAccount("负债账户_" + System.currentTimeMillis(), "liability", new BigDecimal("2000.00")));

        DashboardVO result = dashboardService.getDashboard(testUserId);

        assertNotNull(result);
        assertNotNull(result.getTotalAssets());
        assertNotNull(result.getTotalLiabilities());
        assertNotNull(result.getNetWorth());
        assertNotNull(result.getAccountCount());
    }

    @Test
    void getDashboard_shouldCalculateNetWorthCorrectly() {
        accountMapper.insert(createAccount("资产账户A_" + System.currentTimeMillis(), "asset", new BigDecimal("10000.00")));
        accountMapper.insert(createAccount("负债账户B_" + System.currentTimeMillis(), "liability", new BigDecimal("3000.00")));
        accountMapper.insert(createAccount("投资账户C_" + System.currentTimeMillis(), "investment", new BigDecimal("5000.00")));

        DashboardVO result = dashboardService.getDashboard(testUserId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getTotalAssets()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(result.getTotalLiabilities()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getTotalInvestments()));
        assertEquals(0, new BigDecimal("12000.00").compareTo(result.getNetWorth()));
    }

    @Test
    void getDashboard_shouldReturnZeroWhenNoAccounts() {
        DashboardVO result = dashboardService.getDashboard(testUserId);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalAssets()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalLiabilities()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getNetWorth()));
        assertTrue(result.getAccountCount() >= 10);
    }

    @Test
    void getDashboard_shouldIncludeAllAccountTypes() {
        accountMapper.insert(createAccount("资产_" + System.currentTimeMillis(), "asset", new BigDecimal("5000.00")));
        accountMapper.insert(createAccount("负债_" + System.currentTimeMillis(), "liability", new BigDecimal("1500.00")));
        accountMapper.insert(createAccount("投资_" + System.currentTimeMillis(), "investment", new BigDecimal("3000.00")));

        DashboardVO result = dashboardService.getDashboard(testUserId);

        assertNotNull(result);
        assertEquals(13, result.getAccountCount());
    }

    @Test
    void getNetWorthTrend_shouldReturnTrend() {
        accountMapper.insert(createAccount("资产趋势_" + System.currentTimeMillis(), "asset", new BigDecimal("8000.00")));

        Map<String, Object> result = dashboardService.getNetWorthTrend(testUserId, "2026-01-01", "2026-05-23");

        assertNotNull(result);
        assertNotNull(result.get("startNetWorth"));
        assertNotNull(result.get("currentNetWorth"));
        assertNotNull(result.get("change"));
    }

    @Test
    void getNetWorthTrend_shouldReturnZeroWhenNoAccounts() {
        Map<String, Object> result = dashboardService.getNetWorthTrend(testUserId, "2026-01-01", "2099-12-31");

        assertNotNull(result);
        assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) result.get("startNetWorth")));
        assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) result.get("currentNetWorth")));
    }

    @Test
    void getNetWorthTrend_shouldCalculateChangeCorrectly() {
        accountMapper.insert(createAccount("起始资产_" + System.currentTimeMillis(), "asset", new BigDecimal("10000.00")));

        Map<String, Object> result = dashboardService.getNetWorthTrend(testUserId, "2026-01-01", "2026-05-23");

        assertNotNull(result);
        BigDecimal change = (BigDecimal) result.get("change");
        assertNotNull(change);
    }

    @Test
    void getMonthlyDetail_shouldReturnMonthlyData() {
        accountMapper.insert(createAccount("月度明细资产_" + System.currentTimeMillis(), "asset", new BigDecimal("6000.00")));

        DashboardVO result = dashboardService.getMonthlyDetail(testUserId, "2026-05");

        assertNotNull(result);
        assertNotNull(result.getTotalIncome());
        assertNotNull(result.getTotalExpense());
        assertNotNull(result.getNetAmount());
    }

    @Test
    void getMonthlyDetail_shouldReturnZeroWhenNoData() {
        DashboardVO result = dashboardService.getMonthlyDetail(testUserId, "2099-12");

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getNetAmount()));
        assertEquals(0, result.getIncomeCount());
        assertEquals(0, result.getExpenseCount());
    }

    @Test
    void getMonthlyDetail_shouldSetTransactionCounts() {
        accountMapper.insert(createAccount("统计账户_" + System.currentTimeMillis(), "asset", new BigDecimal("4000.00")));

        DashboardVO result = dashboardService.getMonthlyDetail(testUserId, "2026-05");

        assertNotNull(result);
        assertNotNull(result.getMonthTransactionCount());
    }

    @Test
    void dashboard_shouldHandleMultipleAccountTypes() {
        accountMapper.insert(createAccount("活期资产_" + System.currentTimeMillis(), "asset", new BigDecimal("20000.00")));
        accountMapper.insert(createAccount("信用卡_" + System.currentTimeMillis(), "liability", new BigDecimal("5000.00")));
        accountMapper.insert(createAccount("股票基金_" + System.currentTimeMillis(), "investment", new BigDecimal("10000.00")));
        accountMapper.insert(createAccount("定期存款_" + System.currentTimeMillis(), "asset", new BigDecimal("15000.00")));

        DashboardVO result = dashboardService.getDashboard(testUserId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("35000.00").compareTo(result.getTotalAssets()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.getTotalLiabilities()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getTotalInvestments()));
        assertEquals(0, new BigDecimal("40000.00").compareTo(result.getNetWorth()));
        assertEquals(14, result.getAccountCount());
    }
}