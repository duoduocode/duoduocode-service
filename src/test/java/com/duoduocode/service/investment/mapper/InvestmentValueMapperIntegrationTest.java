package com.duoduocode.service.investment.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import com.duoduocode.service.investment.entity.InvestmentValue;
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
class InvestmentValueMapperIntegrationTest {

    @Autowired
    private InvestmentValueMapper investmentValueMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_investment_value_" + System.currentTimeMillis());
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

    private InvestmentValue createValue(LocalDate date, BigDecimal marketValue, BigDecimal costBasis) {
        InvestmentValue value = new InvestmentValue();
        value.setAccountId(testAccountId);
        value.setDate(date);
        value.setMarketValue(marketValue);
        value.setCostBasis(costBasis);
        value.setNote("测试市值记录");
        value.setCreatedAt(LocalDateTime.now());
        value.setUpdatedAt(LocalDateTime.now());
        return value;
    }

    @Test
    void insert_shouldSuccess() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));

        int result = investmentValueMapper.insert(value);

        assertEquals(1, result);
        assertNotNull(value.getId());
    }

    @Test
    void updateById_shouldSuccess() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        investmentValueMapper.insert(value);

        value.setMarketValue(new BigDecimal("12000.00"));
        int result = investmentValueMapper.updateById(value);

        assertEquals(1, result);

        InvestmentValue updated = investmentValueMapper.selectById(value.getId());
        assertEquals(0, new BigDecimal("12000.00").compareTo(updated.getMarketValue()));
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        value.setId(99999L);

        int result = investmentValueMapper.updateById(value);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnValue() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        investmentValueMapper.insert(value);

        InvestmentValue result = investmentValueMapper.selectById(value.getId());

        assertNotNull(result);
        assertEquals(value.getId(), result.getId());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        InvestmentValue result = investmentValueMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByAccountId_shouldReturnAllValues() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 18),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 19),
                new BigDecimal("11500.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("12000.00"), new BigDecimal("10000.00")));

        List<InvestmentValue> result = investmentValueMapper.selectByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void selectByAccountId_shouldReturnEmptyListWhenNoValue() {
        List<InvestmentValue> result = investmentValueMapper.selectByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByAccountIdAndDateRange_shouldReturnFilteredValues() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 18),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 19),
                new BigDecimal("11500.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("12000.00"), new BigDecimal("10000.00")));

        List<InvestmentValue> result = investmentValueMapper.selectByAccountIdAndDateRange(
                testAccountId, "2026-05-19", "2026-05-20");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void selectByAccountIdAndDateRange_shouldReturnEmptyWhenNoMatch() {
        List<InvestmentValue> result = investmentValueMapper.selectByAccountIdAndDateRange(
                testAccountId, "2099-01-01", "2099-12-31");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectLatestByAccountId_shouldReturnLatestValue() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 18),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("12000.00"), new BigDecimal("10000.00")));

        InvestmentValue result = investmentValueMapper.selectLatestByAccountId(testAccountId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("12000.00").compareTo(result.getMarketValue()));
    }

    @Test
    void selectLatestByAccountId_shouldReturnNullWhenNoValue() {
        InvestmentValue result = investmentValueMapper.selectLatestByAccountId(testAccountId);

        assertNull(result);
    }

    @Test
    void selectByAccountIdAndDate_shouldReturnValue() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));

        InvestmentValue result = investmentValueMapper.selectByAccountIdAndDate(
                testAccountId, "2026-05-20");

        assertNotNull(result);
    }

    @Test
    void selectByAccountIdAndDate_shouldReturnNullWhenNotExist() {
        InvestmentValue result = investmentValueMapper.selectByAccountIdAndDate(
                testAccountId, "2099-12-31");

        assertNull(result);
    }

    @Test
    void deleteById_shouldSuccess() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        investmentValueMapper.insert(value);

        int result = investmentValueMapper.deleteById(value.getId());

        assertEquals(1, result);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = investmentValueMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void calculateTotalProfit_shouldReturnSum() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 18),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("12000.00"), new BigDecimal("10000.00")));

        BigDecimal result = investmentValueMapper.calculateTotalProfit(testAccountId);

        assertNotNull(result);
    }

    @Test
    void calculateTotalProfit_shouldReturnZeroWhenNoValue() {
        BigDecimal result = investmentValueMapper.calculateTotalProfit(testAccountId);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void calculateLatestProfitRate_shouldReturnRate() {
        investmentValueMapper.insert(createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00")));

        BigDecimal result = investmentValueMapper.calculateLatestProfitRate(testAccountId);

        assertNotNull(result);
    }

    @Test
    void calculateLatestProfitRate_shouldReturnZeroWhenNoValue() {
        BigDecimal result = investmentValueMapper.calculateLatestProfitRate(testAccountId);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void valueLifecycle_shouldWorkCorrectly() {
        InvestmentValue value = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        investmentValueMapper.insert(value);

        InvestmentValue saved = investmentValueMapper.selectById(value.getId());
        assertNotNull(saved);

        saved.setMarketValue(new BigDecimal("12500.00"));
        investmentValueMapper.updateById(saved);

        InvestmentValue updated = investmentValueMapper.selectById(value.getId());
        assertEquals(0, new BigDecimal("12500.00").compareTo(updated.getMarketValue()));

        int deleteResult = investmentValueMapper.deleteById(value.getId());
        assertEquals(1, deleteResult);

        InvestmentValue deleted = investmentValueMapper.selectById(value.getId());
        assertNull(deleted);
    }

    @Test
    void multipleValues_shouldBeIndependent() {
        InvestmentValue value1 = createValue(LocalDate.of(2026, 5, 18),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        InvestmentValue value2 = createValue(LocalDate.of(2026, 5, 20),
                new BigDecimal("12000.00"), new BigDecimal("10000.00"));
        investmentValueMapper.insert(value1);
        investmentValueMapper.insert(value2);

        InvestmentValue result1 = investmentValueMapper.selectById(value1.getId());
        InvestmentValue result2 = investmentValueMapper.selectById(value2.getId());

        assertNotEquals(result1.getMarketValue(), result2.getMarketValue());
    }
}