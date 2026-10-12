package com.duoduocode.service.report.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R004 U412：selectAccountBalancesAsOf 期末聚合与 dashboard 当前净资产口径一致性防线
 * （期初/期末与 DashboardService 同一分录聚合口径，防 as-of 漂移）。
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class MonthlyReportMapperIntegrationTest {

    @Autowired
    private MonthlyReportMapper monthlyReportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private EntryMapper entryMapper;

    private Long testUserId;
    private Long assetAccountId;
    private Long liabilityAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_r004_mapper_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        assetAccountId = insertAccount("R004资产", "asset", new BigDecimal("1000.00"), null);
        liabilityAccountId = insertAccount("R004负债", "liability", new BigDecimal("-500.00"), new BigDecimal("500.00"));

        Category category = new Category();
        category.setUserId(testUserId);
        category.setName("R004测试分类_" + System.currentTimeMillis());
        category.setType("expense");
        category.setIcon("🍔");
        category.setSortOrder(0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);

        // 2026-07-15：收入 2000（资产方借方 +2000）；2026-07-20：支出 300（资产方贷方 −300）
        Long incomeCat = insertCategory("R004收入", "income");
        insertEntryTransaction("2026-07-15", "income", new BigDecimal("2000.00"), incomeCat);
        insertEntryTransaction("2026-07-20", "expense", new BigDecimal("300.00"), category.getId());
    }

    private Long insertAccount(String name, String type, BigDecimal initial, BigDecimal creditLimit) {
        Account account = new Account();
        account.setUserId(testUserId);
        account.setName(name + "_" + System.currentTimeMillis());
        account.setType(type);
        account.setIcon("💰");
        account.setColor("#07C160");
        account.setInitialBalance(initial);
        account.setCreditLimit(creditLimit);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(BigDecimal.ZERO);
        account.setSortOrder(0);
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        return account.getId();
    }

    private Long insertCategory(String name, String type) {
        Category category = new Category();
        category.setUserId(testUserId);
        category.setName(name + "_" + System.currentTimeMillis());
        category.setType(type);
        category.setIcon("📁");
        category.setSortOrder(0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }

    private void insertEntryTransaction(String date, String transactionType, BigDecimal amount, Long categoryId) {
        Transaction transaction = new Transaction();
        transaction.setUserId(testUserId);
        transaction.setDate(LocalDate.parse(date));
        transaction.setTime(LocalTime.NOON);
        transaction.setAmount(amount);
        transaction.setDescription("R004口径测试_" + System.currentTimeMillis());
        transaction.setMode("simple");
        transaction.setTransactionType(transactionType);
        transaction.setRefundStatus("none");
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setIsDeleted(0);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);

        // 分类分录：income 借贷方向与 expense 相反（收入=贷方）
        Entry categoryEntry = new Entry();
        categoryEntry.setTransactionId(transaction.getId());
        categoryEntry.setAccountId(categoryId);
        categoryEntry.setDebit("expense".equals(transactionType) ? amount : null);
        categoryEntry.setCredit("income".equals(transactionType) ? amount : null);
        categoryEntry.setAccountType("category");
        categoryEntry.setIsDeleted(0);
        categoryEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(categoryEntry);

        // 资产分录：income 借方 +amount / expense 贷方 −amount
        Entry accountEntry = new Entry();
        accountEntry.setTransactionId(transaction.getId());
        accountEntry.setAccountId(assetAccountId);
        accountEntry.setDebit("income".equals(transactionType) ? amount : null);
        accountEntry.setCredit("expense".equals(transactionType) ? amount : null);
        accountEntry.setAccountType("account");
        accountEntry.setIsDeleted(0);
        accountEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(accountEntry);
    }

    private BigDecimal netWorth(List<Account> accounts) {
        BigDecimal net = BigDecimal.ZERO;
        for (Account a : accounts) {
            if (Boolean.TRUE.equals(a.getIncludeInNetWorth()) && a.getCurrentBalance() != null) {
                net = net.add(a.getCurrentBalance());
            }
        }
        return net;
    }

    private Account findAccount(List<Account> accounts, Long id) {
        return accounts.stream().filter(a -> id.equals(a.getId())).findFirst().orElse(null);
    }

    @Test
    void U412_期末聚合与dashboard当前净资产一致() {
        // as-of 2026-07-31（期末）：资产 1000+2000−300=2700，负债 −500
        List<Account> asOf = monthlyReportMapper.selectAccountBalancesAsOf(testUserId, "2026-07-31");
        assertNotNull(asOf);
        assertEquals(new BigDecimal("2700.00").doubleValue(),
                findAccount(asOf, assetAccountId).getCurrentBalance().doubleValue(), 0.001);
        assertEquals(new BigDecimal("-500.00").doubleValue(),
                findAccount(asOf, liabilityAccountId).getCurrentBalance().doubleValue(), 0.001);

        // dashboard 口径（selectByUserId 当前余额）：无 07-31 之后的交易，两口径必须一致
        List<Account> current = accountMapper.selectByUserId(testUserId);
        assertEquals(netWorth(current).doubleValue(), netWorth(asOf).doubleValue(), 0.001,
                "as-of 期末净资产应与 dashboard 当前净资产一致（无期末后交易时）");
    }

    @Test
    void U412_期初时点_不含期后分录() {
        // as-of 2026-07-01（期初）：仅初始余额，资产 1000，负债 −500
        List<Account> asOf = monthlyReportMapper.selectAccountBalancesAsOf(testUserId, "2026-07-01");
        assertNotNull(asOf);
        assertEquals(new BigDecimal("1000.00").doubleValue(),
                findAccount(asOf, assetAccountId).getCurrentBalance().doubleValue(), 0.001);
        assertEquals(new BigDecimal("-500.00").doubleValue(),
                findAccount(asOf, liabilityAccountId).getCurrentBalance().doubleValue(), 0.001);
        assertEquals(new BigDecimal("500.00").doubleValue(), netWorth(asOf).doubleValue(), 0.001);
    }

    @Test
    void U412_交易当日计入_asOf含端点() {
        // as-of 恰为支出日 2026-07-20：该日分录应计入（≤ date 含端点）
        List<Account> asOf = monthlyReportMapper.selectAccountBalancesAsOf(testUserId, "2026-07-20");
        assertEquals(new BigDecimal("2700.00").doubleValue(),
                findAccount(asOf, assetAccountId).getCurrentBalance().doubleValue(), 0.001);
    }
}
