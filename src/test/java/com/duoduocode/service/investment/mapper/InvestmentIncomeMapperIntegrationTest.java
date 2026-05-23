package com.duoduocode.service.investment.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import com.duoduocode.service.investment.entity.InvestmentIncome;
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
class InvestmentIncomeMapperIntegrationTest {

    @Autowired
    private InvestmentIncomeMapper investmentIncomeMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_investment_income_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        Account account = new Account();
        account.setUserId(testUserId);
        account.setName("投资账户_" + System.currentTimeMillis());
        account.setType("investment");
        account.setIcon("📈");
        account.setColor("#FF5733");
        account.setInitialBalance(new BigDecimal("10000.00"));
        account.setCreditLimit(BigDecimal.ZERO);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(new BigDecimal("0.8"));
        account.setSortOrder(0);
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        testAccountId = account.getId();
    }

    private InvestmentIncome createIncome(String type, BigDecimal amount) {
        InvestmentIncome income = new InvestmentIncome();
        income.setAccountId(testAccountId);
        income.setDate(LocalDate.of(2026, 5, 20));
        income.setAmount(amount);
        income.setType(type);
        income.setNote("测试收益");
        income.setIsReinvested(0);
        income.setCreatedAt(LocalDateTime.now());
        income.setUpdatedAt(LocalDateTime.now());
        return income;
    }

    @Test
    void insert_shouldSuccess() {
        InvestmentIncome income = createIncome("daily", new BigDecimal("100.00"));

        int result = investmentIncomeMapper.insert(income);

        assertEquals(1, result);
        assertNotNull(income.getId());
    }

    @Test
    void updateById_shouldSuccess() {
        InvestmentIncome income = createIncome("dividend", new BigDecimal("200.00"));
        investmentIncomeMapper.insert(income);

        income.setAmount(new BigDecimal("300.00"));
        int result = investmentIncomeMapper.updateById(income);

        assertEquals(1, result);

        InvestmentIncome updated = investmentIncomeMapper.selectById(income.getId());
        assertEquals(0, new BigDecimal("300.00").compareTo(updated.getAmount()));
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        InvestmentIncome income = createIncome("daily", new BigDecimal("100.00"));
        income.setId(99999L);

        int result = investmentIncomeMapper.updateById(income);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnIncome() {
        InvestmentIncome income = createIncome("maturity", new BigDecimal("500.00"));
        investmentIncomeMapper.insert(income);

        InvestmentIncome result = investmentIncomeMapper.selectById(income.getId());

        assertNotNull(result);
        assertEquals(income.getId(), result.getId());
        assertEquals("maturity", result.getType());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        InvestmentIncome result = investmentIncomeMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByAccountId_shouldReturnAllIncomes() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("100.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("200.00")));
        investmentIncomeMapper.insert(createIncome("maturity", new BigDecimal("300.00")));

        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void selectByAccountId_shouldReturnEmptyListWhenNoIncome() {
        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByAccountIdAndDateRange_shouldReturnFilteredIncomes() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("100.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("200.00")));

        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountIdAndDateRange(
                testAccountId, "2026-05-01", "2026-05-31");

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void selectByAccountIdAndDateRange_shouldReturnEmptyWhenNoMatch() {
        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountIdAndDateRange(
                testAccountId, "2099-01-01", "2099-12-31");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByAccountIdAndMonth_shouldReturnMonthlyIncomes() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("100.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("200.00")));

        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountIdAndMonth(
                testAccountId, "2026-05");

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void selectByAccountIdAndMonth_shouldReturnEmptyWhenNoMatch() {
        List<InvestmentIncome> result = investmentIncomeMapper.selectByAccountIdAndMonth(
                testAccountId, "2099-12");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void calculateTotalIncome_shouldReturnSum() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("100.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("200.00")));
        investmentIncomeMapper.insert(createIncome("maturity", new BigDecimal("300.00")));

        BigDecimal result = investmentIncomeMapper.calculateTotalIncome(testAccountId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("600.00").compareTo(result));
    }

    @Test
    void calculateTotalIncome_shouldReturnZeroWhenNoIncome() {
        BigDecimal result = investmentIncomeMapper.calculateTotalIncome(testAccountId);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateMonthlyIncome_shouldReturnSum() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("150.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("250.00")));

        BigDecimal result = investmentIncomeMapper.calculateMonthlyIncome(testAccountId, "2026-05");

        assertNotNull(result);
        assertEquals(0, new BigDecimal("400.00").compareTo(result));
    }

    @Test
    void calculateMonthlyIncome_shouldReturnZeroWhenNoMatch() {
        BigDecimal result = investmentIncomeMapper.calculateMonthlyIncome(testAccountId, "2099-12");

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateIncomeByType_shouldReturnFilteredSum() {
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("100.00")));
        investmentIncomeMapper.insert(createIncome("dividend", new BigDecimal("200.00")));
        investmentIncomeMapper.insert(createIncome("daily", new BigDecimal("150.00")));

        BigDecimal result = investmentIncomeMapper.calculateIncomeByType(testAccountId, "daily");

        assertNotNull(result);
        assertEquals(0, new BigDecimal("250.00").compareTo(result));
    }

    @Test
    void calculateIncomeByType_shouldReturnZeroWhenNoMatch() {
        BigDecimal result = investmentIncomeMapper.calculateIncomeByType(testAccountId, "maturity");

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void deleteById_shouldSuccess() {
        InvestmentIncome income = createIncome("daily", new BigDecimal("50.00"));
        investmentIncomeMapper.insert(income);

        int result = investmentIncomeMapper.deleteById(income.getId());

        assertEquals(1, result);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = investmentIncomeMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void incomeLifecycle_shouldWorkCorrectly() {
        InvestmentIncome income = createIncome("daily", new BigDecimal("100.00"));
        investmentIncomeMapper.insert(income);

        InvestmentIncome saved = investmentIncomeMapper.selectById(income.getId());
        assertNotNull(saved);

        saved.setAmount(new BigDecimal("200.00"));
        investmentIncomeMapper.updateById(saved);

        InvestmentIncome updated = investmentIncomeMapper.selectById(income.getId());
        assertEquals(0, new BigDecimal("200.00").compareTo(updated.getAmount()));

        int deleteResult = investmentIncomeMapper.deleteById(income.getId());
        assertEquals(1, deleteResult);

        InvestmentIncome deleted = investmentIncomeMapper.selectById(income.getId());
        assertNull(deleted);
    }

    @Test
    void multipleIncomes_shouldBeIndependent() {
        InvestmentIncome income1 = createIncome("daily", new BigDecimal("100.00"));
        InvestmentIncome income2 = createIncome("dividend", new BigDecimal("200.00"));
        investmentIncomeMapper.insert(income1);
        investmentIncomeMapper.insert(income2);

        InvestmentIncome result1 = investmentIncomeMapper.selectById(income1.getId());
        InvestmentIncome result2 = investmentIncomeMapper.selectById(income2.getId());

        assertNotEquals(result1.getType(), result2.getType());
        assertEquals(0, new BigDecimal("100.00").compareTo(result1.getAmount()));
        assertEquals(0, new BigDecimal("200.00").compareTo(result2.getAmount()));
    }
}