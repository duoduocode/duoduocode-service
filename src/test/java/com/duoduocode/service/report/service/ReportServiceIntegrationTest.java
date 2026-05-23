package com.duoduocode.service.report.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.report.dto.*;
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
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

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
        user.setOpenid("test_openid_report_service_" + System.currentTimeMillis());
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
        category.setAlertThreshold(new BigDecimal("0.8"));
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

    private void createEntry(Long transactionId, Long categoryId, Long accountId, BigDecimal debit, BigDecimal credit) {
        Entry debitEntry = new Entry();
        debitEntry.setTransactionId(transactionId);
        debitEntry.setAccountId(categoryId);
        debitEntry.setDebit(debit);
        debitEntry.setCredit(null);
        debitEntry.setAccountType("category");
        debitEntry.setIsDeleted(0);
        debitEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(debitEntry);

        Entry creditEntry = new Entry();
        creditEntry.setTransactionId(transactionId);
        creditEntry.setAccountId(accountId);
        creditEntry.setDebit(null);
        creditEntry.setCredit(credit);
        creditEntry.setAccountType("account");
        creditEntry.setIsDeleted(0);
        creditEntry.setCreatedAt(LocalDateTime.now());
        entryMapper.insert(creditEntry);
    }

    @Test
    void getIncomeExpenseReport_shouldReturnEmptyReportWhenNoData() {
        IncomeExpenseReportVO report = reportService.getIncomeExpenseReport(testUserId, "2024-01-01", "2024-12-31");

        assertNotNull(report);
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getBalance()));
        assertNotNull(report.getMonthlyData());
        assertTrue(report.getMonthlyData().isEmpty());
        assertNotNull(report.getTopExpenseCategories());
    }

    @Test
    void getIncomeExpenseReport_shouldCalculateTotalsCorrectly() {
        Transaction expenseTransaction = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction.getId(), testCategoryId, testAccountId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        IncomeExpenseReportVO report = reportService.getIncomeExpenseReport(testUserId, "2024-06-01", "2024-06-30");

        assertNotNull(report);
    }

    @Test
    void getAccountTrendReport_shouldReturnEmptyReportWhenNoData() {
        AccountTrendReportVO report = reportService.getAccountTrendReport(testUserId, testAccountId, "2024-01-01", "2024-12-31");

        assertNotNull(report);
        assertEquals(testAccountId, report.getAccountId());
        assertEquals("", report.getAccountName());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getMaxBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getMinBalance()));
    }

    @Test
    void getAccountTrendReport_shouldCalculateBalanceRange() {
        Transaction expenseTransaction = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction.getId(), testCategoryId, testAccountId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        AccountTrendReportVO report = reportService.getAccountTrendReport(testUserId, testAccountId, "2024-06-01", "2024-06-30");

        assertNotNull(report);
    }

    @Test
    void getCategoryAnalysisReport_shouldReturnEmptyListWhenNoData() {
        List<CategoryAnalysisReportVO> result = reportService.getCategoryAnalysisReport(
                testUserId, "expense", "2024-01-01", "2024-12-31");

        assertNotNull(result);
    }

    @Test
    void getCategoryAnalysisReport_shouldCalculatePercentages() {
        Transaction expenseTransaction = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction.getId(), testCategoryId, testAccountId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        List<CategoryAnalysisReportVO> result = reportService.getCategoryAnalysisReport(
                testUserId, "expense", "2024-06-01", "2024-06-30");

        assertNotNull(result);
    }

    @Test
    void getMonthlyComparisonReport_shouldReturnComparisonData() {
        MonthlyComparisonVO report = reportService.getMonthlyComparisonReport(testUserId, "2024-06", "2024-07");

        assertNotNull(report);
        assertEquals("2024-06", report.getMonth1());
        assertEquals("2024-07", report.getMonth2());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getIncome1()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getIncome2()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getExpense1()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getExpense2()));
        assertNotNull(report.getIncomeChange());
        assertNotNull(report.getExpenseChange());
    }

    @Test
    void getMonthlyComparisonReport_shouldCalculateChangeValues() {
        Transaction expenseTransaction1 = createTransaction("2024-06-15", "expense", new BigDecimal("100.00"));
        createEntry(expenseTransaction1.getId(), testCategoryId, testAccountId, new BigDecimal("100.00"), new BigDecimal("100.00"));

        Transaction expenseTransaction2 = createTransaction("2024-07-15", "expense", new BigDecimal("200.00"));
        createEntry(expenseTransaction2.getId(), testCategoryId, testAccountId, new BigDecimal("200.00"), new BigDecimal("200.00"));

        MonthlyComparisonVO report = reportService.getMonthlyComparisonReport(testUserId, "2024-06", "2024-07");

        assertNotNull(report);
    }
}