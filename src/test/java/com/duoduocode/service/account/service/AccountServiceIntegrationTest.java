package com.duoduocode.service.account.service;

import com.duoduocode.service.account.dto.AccountStatisticsVO;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.transaction.dto.TransactionVO;
import com.duoduocode.service.transaction.entity.Entry;
import com.duoduocode.service.transaction.entity.Transaction;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
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
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private EntryMapper entryMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private Map<String, Object> createDto(String name, String type) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", name);
        dto.put("type", type);
        return dto;
    }

    private Map<String, Object> createDto(String name, String type, Object initialBalance) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", name);
        dto.put("type", type);
        dto.put("initialBalance", initialBalance);
        return dto;
    }

    @Test
    void getAccountList_shouldReturnGroupedAccounts() {
        Map<String, Object> result = accountService.getAccountList(testUserId);

        assertNotNull(result);
        assertNotNull(result.get("accounts"));
        assertNotNull(result.get("summary"));
    }

    @Test
    void createAccount_shouldSuccess() {
        Map<String, Object> dto = createDto("测试账户_" + System.currentTimeMillis(), "asset");
        dto.put("initialBalance", 1000);
        dto.put("icon", "💰");
        dto.put("color", "#07C160");

        Long accountId = accountService.createAccount(testUserId, dto);

        assertNotNull(accountId);
    }

    @Test
    void createAccount_shouldThrowExceptionWhenNameEmpty() {
        Map<String, Object> dto = createDto("", "asset");

        assertThrows(Exception.class, () -> {
            accountService.createAccount(testUserId, dto);
        });
    }

    @Test
    void createAccount_shouldAllowDuplicateName() {
        String testName = "重名账户_" + System.currentTimeMillis();

        accountService.createAccount(testUserId, createDto(testName, "asset"));
        Map<String, Object> dto2 = createDto(testName, "asset");

        assertDoesNotThrow(() -> {
            accountService.createAccount(testUserId, dto2);
        });
    }

    @Test
    void updateAccount_shouldSuccess() {
        String uniqueName = "更新账户_" + System.currentTimeMillis();

        Long accountId = accountService.createAccount(testUserId, createDto(uniqueName, "asset", 1000));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("name", uniqueName + "_updated");
        updateDto.put("color", "#FF0000");

        assertDoesNotThrow(() -> {
            accountService.updateAccount(testUserId, accountId, updateDto);
        });
    }

    @Test
    void deleteAccount_shouldSuccess() {
        Long accountId = accountService.createAccount(testUserId, createDto("删除账户_" + System.currentTimeMillis(), "asset"));

        assertDoesNotThrow(() -> {
            accountService.deleteAccount(testUserId, accountId);
        });
    }

    @Test
    void getAccountDetail_shouldReturnAccountInfo() {
        String uniqueName = "详情账户_" + System.currentTimeMillis();
        Long accountId = accountService.createAccount(testUserId, createDto(uniqueName, "asset"));

        Map<String, Object> detail = accountService.getAccountDetail(accountId);

        assertNotNull(detail);
        assertEquals(accountId, detail.get("id"));
        assertEquals(uniqueName, detail.get("name"));
    }

    @Test
    void adjustBalance_shouldSuccess() {
        Long accountId = accountService.createAccount(testUserId, createDto("调整余额账户_" + System.currentTimeMillis(), "asset", 1000));

        assertDoesNotThrow(() -> {
            accountService.adjustBalance(accountId, new BigDecimal("2000.00"), "测试调整");
        });
    }

    @Test
    void getAccountTransactions_shouldReturnPageResult() {
        Long accountId = accountService.createAccount(testUserId, createDto("交易账户_" + System.currentTimeMillis(), "asset", 1000));

        PageResult<TransactionVO> result = accountService.getAccountTransactions(accountId, 1, 10);

        assertNotNull(result);
        assertNotNull(result.getList());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void createAccount_shouldThrowExceptionWhenTypeEmpty() {
        Map<String, Object> dto = createDto("测试账户_" + System.currentTimeMillis(), "");
        dto.put("initialBalance", 1000);

        assertThrows(Exception.class, () -> {
            accountService.createAccount(testUserId, dto);
        });
    }

    @Test
    void updateAccount_shouldThrowExceptionWhenAccountNotExist() {
        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("name", "不存在账户");

        assertThrows(Exception.class, () -> {
            accountService.updateAccount(testUserId, 99999L, updateDto);
        });
    }

    @Test
    void deleteAccount_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            accountService.deleteAccount(testUserId, 99999L);
        });
    }

    @Test
    void getAccountDetail_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            accountService.getAccountDetail(99999L);
        });
    }

    @Test
    void adjustBalance_shouldThrowExceptionWhenAccountNotExist() {
        assertThrows(Exception.class, () -> {
            accountService.adjustBalance(99999L, new BigDecimal("1000"), "测试");
        });
    }

    @Test
    void createAccount_shouldSetDefaultValues() {
        Map<String, Object> dto = createDto("默认值账户_" + System.currentTimeMillis(), "asset");

        Long accountId = accountService.createAccount(testUserId, dto);

        assertNotNull(accountId);
        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals("💰", detail.get("icon"));
        assertEquals("#07C160", detail.get("color"));
        assertEquals(true, detail.get("includeInNetWorth"));
        assertEquals(true, detail.get("allowTransfer"));
        assertEquals(false, detail.get("enableAlert"));
    }

    @Test
    void createAccount_shouldSetCreditLimitForLiability() {
        Map<String, Object> dto = createDto("信用卡_" + System.currentTimeMillis(), "liability");
        dto.put("creditLimit", 5000);

        Long accountId = accountService.createAccount(testUserId, dto);

        assertNotNull(accountId);
        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("5000").compareTo((BigDecimal) detail.get("creditLimit")));
    }

    @Test
    void updateAccount_shouldUpdateMultipleFields() {
        Long accountId = accountService.createAccount(testUserId, createDto("多字段更新_" + System.currentTimeMillis(), "asset"));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("icon", "🏦");
        updateDto.put("color", "#0000FF");
        updateDto.put("includeInNetWorth", false);
        updateDto.put("allowTransfer", false);
        updateDto.put("enableAlert", true);
        updateDto.put("alertThreshold", 0.9);

        assertDoesNotThrow(() -> {
            accountService.updateAccount(testUserId, accountId, updateDto);
        });

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals("🏦", detail.get("icon"));
        assertEquals("#0000FF", detail.get("color"));
        assertEquals(false, detail.get("includeInNetWorth"));
        assertEquals(false, detail.get("allowTransfer"));
        assertEquals(true, detail.get("enableAlert"));
    }

    @Test
    void updateAccount_shouldAllowDuplicateName() {
        String testName = "重复名称_" + System.currentTimeMillis();

        accountService.createAccount(testUserId, createDto(testName, "asset"));
        Long accountId2 = accountService.createAccount(testUserId, createDto(testName + "_2", "asset"));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("name", testName);

        assertDoesNotThrow(() -> {
            accountService.updateAccount(testUserId, accountId2, updateDto);
        });
    }

    @Test
    void updateAccount_shouldIgnoreNullName() {
        Long accountId = accountService.createAccount(testUserId, createDto("忽略空名称_" + System.currentTimeMillis(), "asset"));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("name", "");

        assertDoesNotThrow(() -> {
            accountService.updateAccount(testUserId, accountId, updateDto);
        });
    }

    @Test
    void getAccountList_shouldCalculateNetWorthCorrectly() {
        accountService.createAccount(testUserId, createDto("资产1_" + System.currentTimeMillis(), "asset", 1000));
        accountService.createAccount(testUserId, createDto("资产2_" + System.currentTimeMillis(), "asset", 2000));
        accountService.createAccount(testUserId, createDto("负债_" + System.currentTimeMillis(), "liability", 500));

        Map<String, Object> result = accountService.getAccountList(testUserId);

        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0, new BigDecimal("3000").compareTo((BigDecimal) summary.get("totalAssets")));
        assertEquals(0, new BigDecimal("500").compareTo((BigDecimal) summary.get("totalLiabilities")));
        assertEquals(0, new BigDecimal("2500").compareTo((BigDecimal) summary.get("netWorth")));
    }

    @Test
    void getAccountList_shouldIncludeInvestmentAccounts() {
        accountService.createAccount(testUserId, createDto("投资账户_" + System.currentTimeMillis(), "investment", 5000));

        Map<String, Object> result = accountService.getAccountList(testUserId);

        assertNotNull(result);
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");
        assertEquals(0, new BigDecimal("5000").compareTo((BigDecimal) summary.get("totalInvestments")));
    }

    @Test
    void getAccountDetail_shouldReturnRecentTransactions() {
        Long accountId = accountService.createAccount(testUserId, createDto("交易详情_" + System.currentTimeMillis(), "asset"));

        Map<String, Object> detail = accountService.getAccountDetail(accountId);

        assertNotNull(detail);
        assertNotNull(detail.get("recentTransactions"));
    }

    @Test
    void getAccountTransactions_shouldReturnEmptyWhenNoTransactions() {
        Long accountId = accountService.createAccount(testUserId, createDto("无交易账户_" + System.currentTimeMillis(), "asset"));

        PageResult<TransactionVO> result = accountService.getAccountTransactions(accountId, 1, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void adjustBalance_shouldUpdateBalance() {
        Long accountId = accountService.createAccount(testUserId, createDto("余额调整_" + System.currentTimeMillis(), "asset", 1000));

        accountService.adjustBalance(accountId, new BigDecimal("5000"), "初始化");

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("5000").compareTo((BigDecimal) detail.get("initialBalance")));
    }

    @Test
    void createAccount_shouldSaveDesc() {
        Map<String, Object> dto = createDto("描述账户_" + System.currentTimeMillis(), "asset");
        dto.put("desc", "我的工资卡");

        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals("我的工资卡", detail.get("desc"));
    }

    @Test
    void updateAccount_shouldUpdateDesc() {
        Long accountId = accountService.createAccount(testUserId, createDto("更新描述_" + System.currentTimeMillis(), "asset"));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("desc", "新的账户描述");

        accountService.updateAccount(testUserId, accountId, updateDto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals("新的账户描述", detail.get("desc"));
    }

    @Test
    void getAccountDetail_shouldIncludeDesc() {
        Map<String, Object> dto = createDto("详情描述_" + System.currentTimeMillis(), "asset");
        dto.put("desc", "详细信息描述");

        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertNotNull(detail.get("desc"));
        assertEquals("详细信息描述", detail.get("desc"));
    }

    @Test
    void getAccountStatistics_shouldReturnIncomeAndExpense() {
        Long accountId = accountService.createAccount(testUserId, createDto("统计测试_" + System.currentTimeMillis(), "asset", 0));

        Transaction incomeTx = new Transaction();
        incomeTx.setUserId(testUserId);
        incomeTx.setDate(LocalDate.parse("2026-05-15"));
        incomeTx.setTime(LocalTime.now());
        incomeTx.setAmount(new BigDecimal("500.00"));
        incomeTx.setDescription("测试收入");
        incomeTx.setMode("simple");
        incomeTx.setTransactionType("income");
        incomeTx.setRefundStatus("none");
        incomeTx.setRefundedAmount(BigDecimal.ZERO);
        incomeTx.setIsDeleted(0);
        incomeTx.setCreatedAt(LocalDateTime.now());
        incomeTx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(incomeTx);

        Entry incomeEntry = new Entry();
        incomeEntry.setTransactionId(incomeTx.getId());
        incomeEntry.setAccountId(accountId);
        incomeEntry.setDebit(new BigDecimal("500.00"));
        incomeEntry.setCredit(null);
        incomeEntry.setAccountType("account");
        incomeEntry.setIsDeleted(0);
        incomeEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(incomeEntry);

        Transaction expenseTx = new Transaction();
        expenseTx.setUserId(testUserId);
        expenseTx.setDate(LocalDate.parse("2026-05-20"));
        expenseTx.setTime(LocalTime.now());
        expenseTx.setAmount(new BigDecimal("200.00"));
        expenseTx.setDescription("测试支出");
        expenseTx.setMode("simple");
        expenseTx.setTransactionType("expense");
        expenseTx.setRefundStatus("none");
        expenseTx.setRefundedAmount(BigDecimal.ZERO);
        expenseTx.setIsDeleted(0);
        expenseTx.setCreatedAt(LocalDateTime.now());
        expenseTx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(expenseTx);

        Entry expenseEntry = new Entry();
        expenseEntry.setTransactionId(expenseTx.getId());
        expenseEntry.setAccountId(accountId);
        expenseEntry.setDebit(null);
        expenseEntry.setCredit(new BigDecimal("200.00"));
        expenseEntry.setAccountType("account");
        expenseEntry.setIsDeleted(0);
        expenseEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(expenseEntry);

        AccountStatisticsVO stats = accountService.getAccountStatistics(accountId, "2026-05-01", "2026-05-31");

        assertNotNull(stats);
        assertEquals(0, new BigDecimal("500.00").compareTo(stats.getIncome()));
        assertEquals(0, new BigDecimal("200.00").compareTo(stats.getExpense()));
        assertEquals(2L, stats.getTransactionCount().longValue());
    }

    @Test
    void getAccountStatistics_shouldFilterByDateRange() {
        Long accountId = accountService.createAccount(testUserId, createDto("日期范围测试_" + System.currentTimeMillis(), "asset", 0));

        Transaction txInRange = new Transaction();
        txInRange.setUserId(testUserId);
        txInRange.setDate(LocalDate.parse("2026-05-15"));
        txInRange.setTime(LocalTime.now());
        txInRange.setAmount(new BigDecimal("300.00"));
        txInRange.setDescription("范围内");
        txInRange.setMode("simple");
        txInRange.setTransactionType("income");
        txInRange.setRefundStatus("none");
        txInRange.setRefundedAmount(BigDecimal.ZERO);
        txInRange.setIsDeleted(0);
        txInRange.setCreatedAt(LocalDateTime.now());
        txInRange.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(txInRange);

        Entry entryInRange = new Entry();
        entryInRange.setTransactionId(txInRange.getId());
        entryInRange.setAccountId(accountId);
        entryInRange.setDebit(new BigDecimal("300.00"));
        entryInRange.setAccountType("account");
        entryInRange.setIsDeleted(0);
        entryInRange.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entryInRange);

        Transaction txOutRange = new Transaction();
        txOutRange.setUserId(testUserId);
        txOutRange.setDate(LocalDate.parse("2026-04-28"));
        txOutRange.setTime(LocalTime.now());
        txOutRange.setAmount(new BigDecimal("100.00"));
        txOutRange.setDescription("范围外");
        txOutRange.setMode("simple");
        txOutRange.setTransactionType("income");
        txOutRange.setRefundStatus("none");
        txOutRange.setRefundedAmount(BigDecimal.ZERO);
        txOutRange.setIsDeleted(0);
        txOutRange.setCreatedAt(LocalDateTime.now());
        txOutRange.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(txOutRange);

        Entry entryOutRange = new Entry();
        entryOutRange.setTransactionId(txOutRange.getId());
        entryOutRange.setAccountId(accountId);
        entryOutRange.setDebit(new BigDecimal("100.00"));
        entryOutRange.setAccountType("account");
        entryOutRange.setIsDeleted(0);
        entryOutRange.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entryOutRange);

        AccountStatisticsVO stats = accountService.getAccountStatistics(accountId, "2026-05-01", "2026-05-31");

        assertNotNull(stats);
        assertEquals(0, new BigDecimal("300.00").compareTo(stats.getIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getExpense()));
        assertEquals(1L, stats.getTransactionCount().longValue());
    }

    @Test
    void getAccountStatistics_shouldReturnZeroWhenNoTransactions() {
        Long accountId = accountService.createAccount(testUserId, createDto("空统计_" + System.currentTimeMillis(), "asset", 0));

        AccountStatisticsVO stats = accountService.getAccountStatistics(accountId, "2026-05-01", "2026-05-31");

        assertNotNull(stats);
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getExpense()));
        assertEquals(0L, stats.getTransactionCount().longValue());
    }

    @Test
    void createAccount_liabilityShouldStoreNegativeInitialBalance() {
        Map<String, Object> dto = createDto("信用卡_" + System.currentTimeMillis(), "liability", 3000);
        dto.put("creditLimit", 50000);

        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        BigDecimal currentBalance = (BigDecimal) detail.get("currentBalance");
        assertTrue(currentBalance.compareTo(BigDecimal.ZERO) <= 0,
                "负债账户余额应为非正数，实际: " + currentBalance);
        assertEquals(0, new BigDecimal("-3000").compareTo(currentBalance));
    }

    @Test
    void getAccountList_liabilityShouldReturnDebtAmount() {
        String name = "信用卡_" + System.currentTimeMillis();
        accountService.createAccount(testUserId, createDto(name, "liability", 3000));

        Map<String, Object> result = accountService.getAccountList(testUserId);

        Map<String, Object> accounts = (Map<String, Object>) result.get("accounts");
        java.util.List<Map<String, Object>> liabilityList = (java.util.List<Map<String, Object>>) accounts.get("liability");
        assertFalse(liabilityList.isEmpty());
        Map<String, Object> liab = null;
        for (Map<String, Object> item : liabilityList) {
            if (name.equals(item.get("name"))) {
                liab = item;
                break;
            }
        }
        assertNotNull(liab, "未找到创建的负债账户");
        assertNotNull(liab.get("debtAmount"));
        assertEquals(0, new BigDecimal("3000").compareTo((BigDecimal) liab.get("debtAmount")));
    }

    @Test
    void getAccountDetail_liabilityShouldReturnDebtAmount() {
        Map<String, Object> dto = createDto("信用卡_" + System.currentTimeMillis(), "liability", 3000);
        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);

        assertNotNull(detail.get("debtAmount"));
        assertEquals(0, new BigDecimal("3000").compareTo((BigDecimal) detail.get("debtAmount")));
    }

    @Test
    void adjustBalance_liabilityShouldStoreNegativeBalance() {
        Long accountId = accountService.createAccount(testUserId, createDto("信用调整_" + System.currentTimeMillis(), "liability", 3000));

        accountService.adjustBalance(accountId, new BigDecimal("5000"), "调整负债为5000");

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        BigDecimal currentBalance = (BigDecimal) detail.get("currentBalance");
        assertEquals(0, new BigDecimal("-5000").compareTo(currentBalance));
    }

    @Test
    void createAccount_liabilityWithoutInitialBalanceShouldBeZero() {
        Map<String, Object> dto = createDto("无负债信用卡_" + System.currentTimeMillis(), "liability");

        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        BigDecimal currentBalance = (BigDecimal) detail.get("currentBalance");
        assertEquals(0, BigDecimal.ZERO.compareTo(currentBalance));
        BigDecimal debtAmount = (BigDecimal) detail.get("debtAmount");
        assertEquals(0, BigDecimal.ZERO.compareTo(debtAmount));
    }

    @Test
    void updateAccount_assetWithTransactions_shouldRecalculateCurrentBalance() {
        Long accountId = accountService.createAccount(testUserId, createDto("资产调初始_" + System.currentTimeMillis(), "asset", 10000));

        Transaction tx = new Transaction();
        tx.setUserId(testUserId);
        tx.setDate(LocalDate.parse("2026-06-01"));
        tx.setTime(LocalTime.now());
        tx.setAmount(new BigDecimal("3000.00"));
        tx.setDescription("支出测试");
        tx.setMode("simple");
        tx.setTransactionType("expense");
        tx.setRefundStatus("none");
        tx.setRefundedAmount(BigDecimal.ZERO);
        tx.setIsDeleted(0);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);

        Entry entry = new Entry();
        entry.setTransactionId(tx.getId());
        entry.setAccountId(accountId);
        entry.setCredit(new BigDecimal("3000.00"));
        entry.setAccountType("account");
        entry.setIsDeleted(0);
        entry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entry);

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("initialBalance", 8000);
        accountService.updateAccount(testUserId, accountId, updateDto);

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("5000").compareTo((BigDecimal) detail.get("currentBalance")));
    }

    @Test
    void updateAccount_liabilityWithTransactions_shouldRecalculateCurrentBalance() {
        Map<String, Object> dto = createDto("负债调初始_" + System.currentTimeMillis(), "liability", 3000);
        dto.put("creditLimit", 50000);
        Long accountId = accountService.createAccount(testUserId, dto);

        Transaction tx = new Transaction();
        tx.setUserId(testUserId);
        tx.setDate(LocalDate.parse("2026-06-01"));
        tx.setTime(LocalTime.now());
        tx.setAmount(new BigDecimal("366.88"));
        tx.setDescription("消费测试");
        tx.setMode("simple");
        tx.setTransactionType("expense");
        tx.setRefundStatus("none");
        tx.setRefundedAmount(BigDecimal.ZERO);
        tx.setIsDeleted(0);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);

        Entry entry = new Entry();
        entry.setTransactionId(tx.getId());
        entry.setAccountId(accountId);
        entry.setCredit(new BigDecimal("366.88"));
        entry.setAccountType("account");
        entry.setIsDeleted(0);
        entry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entry);

        Map<String, Object> detailBefore = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("-3366.88").compareTo((BigDecimal) detailBefore.get("currentBalance")));

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("initialBalance", 5000);
        accountService.updateAccount(testUserId, accountId, updateDto);

        Map<String, Object> detailAfter = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("-5366.88").compareTo((BigDecimal) detailAfter.get("currentBalance")));
    }

    @Test
    void updateAccount_liabilityChangeCreditLimit_shouldNotAffectCurrentBalance() {
        Map<String, Object> dto = createDto("负债调额度_" + System.currentTimeMillis(), "liability", 3000);
        dto.put("creditLimit", 50000);
        Long accountId = accountService.createAccount(testUserId, dto);

        Map<String, Object> before = accountService.getAccountDetail(accountId);

        Map<String, Object> updateDto = new HashMap<>();
        updateDto.put("creditLimit", 80000);
        accountService.updateAccount(testUserId, accountId, updateDto);

        Map<String, Object> after = accountService.getAccountDetail(accountId);
        assertEquals(0, ((BigDecimal) before.get("currentBalance")).compareTo((BigDecimal) after.get("currentBalance")));
        assertEquals(0, new BigDecimal("80000").compareTo((BigDecimal) after.get("creditLimit")));
    }

    @Test
    void adjustBalance_assetWithTransactions_shouldRecalculateCurrentBalance() {
        Long accountId = accountService.createAccount(testUserId, createDto("资产调账_" + System.currentTimeMillis(), "asset", 10000));

        Transaction tx = new Transaction();
        tx.setUserId(testUserId);
        tx.setDate(LocalDate.parse("2026-06-01"));
        tx.setTime(LocalTime.now());
        tx.setAmount(new BigDecimal("500.00"));
        tx.setDescription("收入测试");
        tx.setMode("simple");
        tx.setTransactionType("income");
        tx.setRefundStatus("none");
        tx.setRefundedAmount(BigDecimal.ZERO);
        tx.setIsDeleted(0);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(tx);

        Entry entry = new Entry();
        entry.setTransactionId(tx.getId());
        entry.setAccountId(accountId);
        entry.setDebit(new BigDecimal("500.00"));
        entry.setAccountType("account");
        entry.setIsDeleted(0);
        entry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entry);

        accountService.adjustBalance(accountId, new BigDecimal("20000"), "调账测试");

        Map<String, Object> detail = accountService.getAccountDetail(accountId);
        assertEquals(0, new BigDecimal("20500").compareTo((BigDecimal) detail.get("currentBalance")));
    }
}