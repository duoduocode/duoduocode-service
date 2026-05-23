package com.duoduocode.service.investment.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import com.duoduocode.service.investment.dto.InvestmentValueDTO;
import com.duoduocode.service.investment.dto.InvestmentValueVO;
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
class InvestmentValueServiceIntegrationTest {

    @Autowired
    private InvestmentValueService investmentValueService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_investment_value_service_" + System.currentTimeMillis());
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

    private InvestmentValueDTO createValueDTO(LocalDate date, BigDecimal marketValue, BigDecimal costBasis) {
        InvestmentValueDTO dto = new InvestmentValueDTO();
        dto.setDate(date);
        dto.setMarketValue(marketValue);
        dto.setCostBasis(costBasis);
        dto.setNote("测试市值记录");
        return dto;
    }

    @Test
    void recordMarketValue_shouldSuccess() {
        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));

        Long id = investmentValueService.recordMarketValue(testAccountId, dto);

        assertNotNull(id);
    }

    @Test
    void recordMarketValue_shouldThrowExceptionWhenAccountNotExist() {
        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));

        assertThrows(Exception.class, () -> {
            investmentValueService.recordMarketValue(99999L, dto);
        });
    }

    @Test
    void recordMarketValue_shouldThrowExceptionWhenNotInvestmentAccount() {
        Account normalAccount = new Account();
        normalAccount.setUserId(testUserId);
        normalAccount.setName("普通账户_" + System.currentTimeMillis());
        normalAccount.setType("asset");
        normalAccount.setIcon("💰");
        normalAccount.setColor("#FF5733");
        normalAccount.setInitialBalance(new BigDecimal("1000.00"));
        normalAccount.setCreditLimit(BigDecimal.ZERO);
        normalAccount.setIncludeInNetWorth(true);
        normalAccount.setAllowTransfer(true);
        normalAccount.setEnableAlert(false);
        normalAccount.setAlertThreshold(new BigDecimal("0.8"));
        normalAccount.setSortOrder(0);
        normalAccount.setIsDeleted(false);
        normalAccount.setCreatedAt(LocalDateTime.now());
        normalAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(normalAccount);

        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));

        assertThrows(Exception.class, () -> {
            investmentValueService.recordMarketValue(normalAccount.getId(), dto);
        });
    }

    @Test
    void recordMarketValue_shouldThrowExceptionWhenDateEmpty() {
        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        dto.setDate(null);

        assertThrows(Exception.class, () -> {
            investmentValueService.recordMarketValue(testAccountId, dto);
        });
    }

    @Test
    void recordMarketValue_shouldThrowExceptionWhenMarketValueEmpty() {
        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        dto.setMarketValue(null);

        assertThrows(Exception.class, () -> {
            investmentValueService.recordMarketValue(testAccountId, dto);
        });
    }

    @Test
    void recordMarketValue_shouldThrowExceptionWhenDuplicateDate() {
        InvestmentValueDTO dto = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11000.00"), new BigDecimal("10000.00"));
        investmentValueService.recordMarketValue(testAccountId, dto);

        InvestmentValueDTO duplicate = createValueDTO(LocalDate.of(2026, 5, 20),
                new BigDecimal("11500.00"), new BigDecimal("10000.00"));

        assertThrows(Exception.class, () -> {
            investmentValueService.recordMarketValue(testAccountId, duplicate);
        });
    }

    @Test
    void getMarketValueHistory_shouldReturnHistory() {
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 18), new BigDecimal("10500.00"), new BigDecimal("10000.00")));
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 19), new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 20), new BigDecimal("11500.00"), new BigDecimal("10000.00")));

        List<InvestmentValueVO> result = investmentValueService.getMarketValueHistory(
                testAccountId, "2026-05-01", "2026-05-31");

        assertNotNull(result);
        assertTrue(result.size() >= 3);
    }

    @Test
    void getMarketValueHistory_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentValueService.getMarketValueHistory(99999L, "2026-05-01", "2026-05-31");
        });
    }

    @Test
    void getMarketValueHistory_shouldReturnEmptyWhenNoMatch() {
        List<InvestmentValueVO> result = investmentValueService.getMarketValueHistory(
                testAccountId, "2099-01-01", "2099-12-31");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getLatestMarketValue_shouldReturnLatest() {
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 18), new BigDecimal("10500.00"), new BigDecimal("10000.00")));
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 20), new BigDecimal("11500.00"), new BigDecimal("10000.00")));

        InvestmentValueVO result = investmentValueService.getLatestMarketValue(testAccountId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("11500.00").compareTo(result.getMarketValue()));
    }

    @Test
    void getLatestMarketValue_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentValueService.getLatestMarketValue(99999L);
        });
    }

    @Test
    void getLatestMarketValue_shouldReturnNullWhenNoData() {
        InvestmentValueVO result = investmentValueService.getLatestMarketValue(testAccountId);

        assertNull(result);
    }

    @Test
    void calculateProfit_shouldReturnProfitData() {
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 20), new BigDecimal("11000.00"), new BigDecimal("10000.00")));

        Map<String, Object> result = investmentValueService.calculateProfit(testAccountId);

        assertNotNull(result);
        assertNotNull(result.get("latestMarketValue"));
        assertNotNull(result.get("latestCostBasis"));
        assertNotNull(result.get("latestProfit"));
        assertNotNull(result.get("latestProfitRate"));
        assertNotNull(result.get("totalProfit"));
    }

    @Test
    void calculateProfit_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentValueService.calculateProfit(99999L);
        });
    }

    @Test
    void calculateProfit_shouldThrowExceptionWhenNotInvestmentAccount() {
        Account normalAccount = new Account();
        normalAccount.setUserId(testUserId);
        normalAccount.setName("普通账户2_" + System.currentTimeMillis());
        normalAccount.setType("asset");
        normalAccount.setIcon("💰");
        normalAccount.setColor("#FF5733");
        normalAccount.setInitialBalance(new BigDecimal("1000.00"));
        normalAccount.setCreditLimit(BigDecimal.ZERO);
        normalAccount.setIncludeInNetWorth(true);
        normalAccount.setAllowTransfer(true);
        normalAccount.setEnableAlert(false);
        normalAccount.setAlertThreshold(new BigDecimal("0.8"));
        normalAccount.setSortOrder(0);
        normalAccount.setIsDeleted(false);
        normalAccount.setCreatedAt(LocalDateTime.now());
        normalAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(normalAccount);

        assertThrows(Exception.class, () -> {
            investmentValueService.calculateProfit(normalAccount.getId());
        });
    }

    @Test
    void calculateProfit_shouldReturnZeroWhenNoData() {
        Map<String, Object> result = investmentValueService.calculateProfit(testAccountId);

        assertNotNull(result);
        assertEquals(0, ((BigDecimal) result.get("latestMarketValue")).compareTo(BigDecimal.ZERO));
        assertEquals(0, ((BigDecimal) result.get("totalProfit")).compareTo(BigDecimal.ZERO));
    }

    @Test
    void valueService_shouldHandleMultipleRecords() {
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 18), new BigDecimal("10500.00"), new BigDecimal("10000.00")));
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 19), new BigDecimal("11000.00"), new BigDecimal("10000.00")));
        investmentValueService.recordMarketValue(testAccountId,
                createValueDTO(LocalDate.of(2026, 5, 20), new BigDecimal("11500.00"), new BigDecimal("10000.00")));

        List<InvestmentValueVO> history = investmentValueService.getMarketValueHistory(
                testAccountId, "2026-05-01", "2026-05-31");
        assertTrue(history.size() >= 3);

        InvestmentValueVO latest = investmentValueService.getLatestMarketValue(testAccountId);
        assertNotNull(latest);
        assertEquals(0, new BigDecimal("11500.00").compareTo(latest.getMarketValue()));

        Map<String, Object> profit = investmentValueService.calculateProfit(testAccountId);
        assertTrue(((BigDecimal) profit.get("latestMarketValue")).compareTo(BigDecimal.ZERO) > 0);
    }
}