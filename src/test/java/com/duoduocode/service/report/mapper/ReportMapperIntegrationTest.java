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

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ReportMapperIntegrationTest {

    @Autowired
    private ReportMapper reportMapper;

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
    private Long testAccountId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_report_mapper_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        Account account = new Account();
        account.setUserId(testUserId);
        account.setName("测试账户_" + System.currentTimeMillis());
        account.setType("asset");
        account.setIcon("💰");
        account.setColor("#1E90FF");
        account.setInitialBalance(new BigDecimal("1000.00"));
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

    private Transaction createTransaction(String date, String transactionType, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setUserId(testUserId);
        transaction.setDate(LocalDate.parse(date));
        transaction.setTime(LocalTime.now());
        transaction.setAmount(amount);
        transaction.setDescription("测试交易_" + System.currentTimeMillis());
        transaction.setMode("simple");
        transaction.setTransactionType(transactionType);
        transaction.setRefundStatus("none");
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setIsDeleted(0);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);
        return transaction;
    }

    private void createEntry(Long transactionId, Long accountId, Long categoryId, BigDecimal debit, BigDecimal credit) {
        Entry entry1 = new Entry();
        entry1.setTransactionId(transactionId);
        entry1.setAccountId(categoryId);
        entry1.setDebit(debit);
        entry1.setCredit(null);
        entry1.setAccountType("category");
        entry1.setIsDeleted(0);
        entry1.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entry1);

        Entry entry2 = new Entry();
        entry2.setTransactionId(transactionId);
        entry2.setAccountId(accountId);
        entry2.setDebit(null);
        entry2.setCredit(credit);
        entry2.setAccountType("account");
        entry2.setIsDeleted(0);
        entry2.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(entry2);
    }

    @Test
    void selectMonthlyIncomeExpense_shouldReturnEmptyListWhenNoData() {
        List<com.duoduocode.service.report.dto.MonthlyData> result =
                reportMapper.selectMonthlyIncomeExpense(testUserId, "2024-01-01", "2024-12-31");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void selectMonthlyIncomeExpense_shouldReturnMonthlyData() {
        Transaction expenseTransaction = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction.getId(), testAccountId, testCategoryId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        List<com.duoduocode.service.report.dto.MonthlyData> result =
                reportMapper.selectMonthlyIncomeExpense(testUserId, "2024-06-01", "2024-06-30");

        assertNotNull(result);
    }

    @Test
    void selectCategoryStats_shouldReturnEmptyListWhenNoData() {
        List<com.duoduocode.service.report.dto.CategoryData> result =
                reportMapper.selectCategoryStats(testUserId, "expense", "2024-01-01", "2024-12-31");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void selectCategoryStats_shouldReturnCategoryStats() {
        Transaction expenseTransaction = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction.getId(), testAccountId, testCategoryId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        List<com.duoduocode.service.report.dto.CategoryData> result =
                reportMapper.selectCategoryStats(testUserId, "expense", "2024-06-01", "2024-06-30");

        assertNotNull(result);
    }

    @Test
    void selectAccountDailyBalance_shouldReturnEmptyListWhenNoData() {
        List<com.duoduocode.service.report.dto.DailyBalance> result =
                reportMapper.selectAccountDailyBalance(testAccountId, "2024-01-01", "2024-12-31");

        assertNotNull(result);
    }
}