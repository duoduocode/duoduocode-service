package com.duoduocode.service.investment.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import com.duoduocode.service.investment.dto.InvestmentIncomeDTO;
import com.duoduocode.service.investment.dto.InvestmentIncomeVO;
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
class InvestmentIncomeServiceIntegrationTest {

    @Autowired
    private InvestmentIncomeService investmentIncomeService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_investment_income_service_" + System.currentTimeMillis());
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

    private InvestmentIncomeDTO createIncomeDTO(String type, BigDecimal amount) {
        InvestmentIncomeDTO dto = new InvestmentIncomeDTO();
        dto.setDate(LocalDate.of(2026, 5, 20));
        dto.setAmount(amount);
        dto.setType(type);
        dto.setNote("测试收益");
        dto.setIsReinvested(0);
        return dto;
    }

    @Test
    void recordIncome_shouldSuccess() {
        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));

        Long id = investmentIncomeService.recordIncome(testAccountId, dto);

        assertNotNull(id);
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenAccountNotExist() {
        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(99999L, dto);
        });
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenNotInvestmentAccount() {
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

        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(normalAccount.getId(), dto);
        });
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenDateEmpty() {
        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));
        dto.setDate(null);

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(testAccountId, dto);
        });
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenAmountEmpty() {
        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));
        dto.setAmount(null);

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(testAccountId, dto);
        });
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenTypeEmpty() {
        InvestmentIncomeDTO dto = createIncomeDTO("daily", new BigDecimal("100.00"));
        dto.setType(null);

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(testAccountId, dto);
        });
    }

    @Test
    void recordIncome_shouldThrowExceptionWhenTypeInvalid() {
        InvestmentIncomeDTO dto = createIncomeDTO("invalid_type", new BigDecimal("100.00"));

        assertThrows(Exception.class, () -> {
            investmentIncomeService.recordIncome(testAccountId, dto);
        });
    }

    @Test
    void recordIncome_shouldAcceptValidTypes() {
        String[] validTypes = {"daily", "dividend", "maturity"};

        for (String type : validTypes) {
            InvestmentIncomeDTO dto = createIncomeDTO(type, new BigDecimal("100.00"));
            dto.setDate(LocalDate.of(2026, 5, (int)(System.currentTimeMillis() % 28 + 1)));

            Long id = investmentIncomeService.recordIncome(testAccountId, dto);
            assertNotNull(id);
        }
    }

    @Test
    void getIncomeHistory_shouldReturnHistory() {
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("daily", new BigDecimal("100.00")));
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("dividend", new BigDecimal("200.00")));

        List<InvestmentIncomeVO> result = investmentIncomeService.getIncomeHistory(
                testAccountId, "2026-05-01", "2026-05-31");

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void getIncomeHistory_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentIncomeService.getIncomeHistory(99999L, "2026-05-01", "2026-05-31");
        });
    }

    @Test
    void getIncomeHistory_shouldReturnEmptyWhenNoMatch() {
        List<InvestmentIncomeVO> result = investmentIncomeService.getIncomeHistory(
                testAccountId, "2099-01-01", "2099-12-31");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getMonthlyIncome_shouldReturnMonthlyData() {
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("daily", new BigDecimal("100.00")));
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("dividend", new BigDecimal("200.00")));

        Map<String, Object> result = investmentIncomeService.getMonthlyIncome(testAccountId, "2026-05");

        assertNotNull(result);
        assertEquals("2026-05", result.get("month"));
        assertNotNull(result.get("totalIncome"));
        assertNotNull(result.get("details"));
    }

    @Test
    void getMonthlyIncome_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentIncomeService.getMonthlyIncome(99999L, "2026-05");
        });
    }

    @Test
    void getMonthlyIncome_shouldReturnZeroWhenNoData() {
        Map<String, Object> result = investmentIncomeService.getMonthlyIncome(testAccountId, "2099-12");

        assertNotNull(result);
        assertEquals("2099-12", result.get("month"));
        assertEquals(0, ((BigDecimal) result.get("totalIncome")).compareTo(BigDecimal.ZERO));
    }

    @Test
    void getTotalIncome_shouldReturnTotalData() {
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("daily", new BigDecimal("100.00")));
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("dividend", new BigDecimal("200.00")));

        Map<String, Object> result = investmentIncomeService.getTotalIncome(testAccountId);

        assertNotNull(result);
        assertNotNull(result.get("totalIncome"));
        assertNotNull(result.get("incomeByType"));
        assertNotNull(result.get("details"));
    }

    @Test
    void getTotalIncome_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            investmentIncomeService.getTotalIncome(99999L);
        });
    }

    @Test
    void getTotalIncome_shouldThrowExceptionWhenNotInvestmentAccount() {
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
            investmentIncomeService.getTotalIncome(normalAccount.getId());
        });
    }

    @Test
    void incomeService_shouldHandleMultipleRecords() {
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("daily", new BigDecimal("100.00")));
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("dividend", new BigDecimal("200.00")));
        investmentIncomeService.recordIncome(testAccountId, createIncomeDTO("maturity", new BigDecimal("300.00")));

        List<InvestmentIncomeVO> history = investmentIncomeService.getIncomeHistory(
                testAccountId, "2026-05-01", "2026-05-31");
        assertTrue(history.size() >= 3);

        Map<String, Object> monthly = investmentIncomeService.getMonthlyIncome(testAccountId, "2026-05");
        assertTrue(((BigDecimal) monthly.get("totalIncome")).compareTo(BigDecimal.ZERO) > 0);

        Map<String, Object> total = investmentIncomeService.getTotalIncome(testAccountId);
        assertTrue(((BigDecimal) total.get("totalIncome")).compareTo(BigDecimal.ZERO) > 0);
    }
}