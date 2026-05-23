package com.duoduocode.service.transaction.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.transaction.dto.TransactionSearchQuery;
import com.duoduocode.service.transaction.entity.Transaction;
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
class TransactionMapperIntegrationTest {

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private Long testUserId;
    private Long testAccountId;
    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_transaction_mapper_" + System.currentTimeMillis());
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
        category.setMonthlyBudget(new BigDecimal("1000.00"));
        category.setAlertThreshold(new BigDecimal("0.8"));
        category.setSortOrder(0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        testCategoryId = category.getId();
    }

    private Transaction createTransaction(String amount, String date) {
        Transaction transaction = new Transaction();
        transaction.setUserId(testUserId);
        transaction.setDate(LocalDate.parse(date));
        transaction.setTime(LocalTime.now());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription("测试交易_" + System.currentTimeMillis());
        transaction.setMode("simple");
        transaction.setTransactionType("expense");
        transaction.setRefundStatus("none");
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setIsDeleted(0);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        return transaction;
    }

    @Test
    void insert_shouldSuccess() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");

        int result = transactionMapper.insert(transaction);

        assertEquals(1, result);
        assertNotNull(transaction.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        Transaction transaction = createTransaction("200.00", "2024-01-02");
        transaction.setTransactionType("expense");

        transactionMapper.insert(transaction);

        Transaction saved = transactionMapper.selectById(transaction.getId());
        assertNotNull(saved);
        assertEquals(testUserId, saved.getUserId());
        assertEquals("2024-01-02", saved.getDate().toString());
        assertEquals(0, new BigDecimal("200.00").compareTo(saved.getAmount()));
        assertEquals("simple", saved.getMode());
        assertEquals("none", saved.getRefundStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getRefundedAmount()));
        assertEquals(0, saved.getIsDeleted());
    }

    @Test
    void updateById_shouldSuccess() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        transaction.setAmount(new BigDecimal("300.00"));
        transaction.setDescription("更新后的描述");

        int result = transactionMapper.updateById(transaction);

        assertEquals(1, result);

        Transaction updated = transactionMapper.selectById(transaction.getId());
        assertEquals(0, new BigDecimal("300.00").compareTo(updated.getAmount()));
        assertEquals("更新后的描述", updated.getDescription());
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transaction.setId(99999L);

        int result = transactionMapper.updateById(transaction);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnTransaction() {
        Transaction transaction = createTransaction("150.00", "2024-01-03");
        transactionMapper.insert(transaction);

        Transaction result = transactionMapper.selectById(transaction.getId());

        assertNotNull(result);
        assertEquals(transaction.getId(), result.getId());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.getAmount()));
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        Transaction result = transactionMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectById_shouldReturnNullWhenDeleted() {
        Transaction transaction = createTransaction("150.00", "2024-01-03");
        transaction.setIsDeleted(1);
        transactionMapper.insert(transaction);

        Transaction result = transactionMapper.selectById(transaction.getId());

        assertNull(result);
    }

    @Test
    void selectByUserId_shouldReturnTransactions() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        List<Transaction> results = transactionMapper.selectByUserId(testUserId, 0, 10, null, null);

        assertNotNull(results);
        assertTrue(results.size() >= 2);
    }

    @Test
    void selectByUserId_shouldFilterByDateRange() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        Transaction transaction2 = createTransaction("200.00", "2024-01-15");
        Transaction transaction3 = createTransaction("300.00", "2024-02-01");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);
        transactionMapper.insert(transaction3);

        List<Transaction> results = transactionMapper.selectByUserId(testUserId, 0, 10, "2024-01-01", "2024-01-31");

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void selectByUserId_shouldRespectLimitAndOffset() {
        for (int i = 0; i < 5; i++) {
            Transaction transaction = createTransaction("100.00", "2024-01-" + String.format("%02d", i + 1));
            transactionMapper.insert(transaction);
        }

        List<Transaction> results = transactionMapper.selectByUserId(testUserId, 2, 2, null, null);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void countByUserId_shouldReturnCorrectCount() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        Long count = transactionMapper.countByUserId(testUserId, null, null);

        assertNotNull(count);
        assertTrue(count >= 2);
    }

    @Test
    void countByUserId_shouldFilterByDateRange() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        Transaction transaction2 = createTransaction("200.00", "2024-02-01");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        Long count = transactionMapper.countByUserId(testUserId, "2024-01-01", "2024-01-31");

        assertNotNull(count);
        assertEquals(1, count);
    }

    @Test
    void selectRecentByUserId_shouldReturnRecentTransactions() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        Transaction transaction2 = createTransaction("200.00", "2024-01-15");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        List<Transaction> results = transactionMapper.selectRecentByUserId(testUserId, 10);

        assertNotNull(results);
        assertTrue(results.size() >= 2);
    }

    @Test
    void selectRecentByUserId_shouldRespectLimit() {
        for (int i = 0; i < 5; i++) {
            Transaction transaction = createTransaction("100.00", "2024-01-" + String.format("%02d", i + 1));
            transactionMapper.insert(transaction);
        }

        List<Transaction> results = transactionMapper.selectRecentByUserId(testUserId, 3);

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void softDeleteById_shouldMarkAsDeleted() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        int result = transactionMapper.softDeleteById(transaction.getId());

        assertEquals(1, result);

        Transaction deleted = transactionMapper.selectById(transaction.getId());
        assertNull(deleted);
    }

    @Test
    void softDeleteById_shouldReturnZeroWhenNotExist() {
        int result = transactionMapper.softDeleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void checkDuplicate_shouldReturnNullWhenNoDuplicate() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        Transaction result = transactionMapper.checkDuplicate(testUserId, "200.00", "2024-01-01", "不同的描述");

        assertNull(result);
    }

    @Test
    void checkDuplicate_shouldReturnTransactionWhenDuplicate() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transaction.setDescription("相同描述");
        transactionMapper.insert(transaction);

        Transaction result = transactionMapper.checkDuplicate(testUserId, "100.00", "2024-01-01", "相同描述");

        assertNotNull(result);
        assertEquals(transaction.getId(), result.getId());
    }

    @Test
    void search_shouldReturnMatchingTransactions() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        transaction1.setDescription("早餐消费");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transaction2.setDescription("午餐消费");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        List<Transaction> results = transactionMapper.search(testUserId, "早餐", null, null, null, null, null, 0, 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getDescription().contains("早餐"));
    }

    @Test
    void search_shouldFilterByAmountRange() {
        Transaction transaction1 = createTransaction("50.00", "2024-01-01");
        Transaction transaction2 = createTransaction("150.00", "2024-01-02");
        Transaction transaction3 = createTransaction("250.00", "2024-01-03");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);
        transactionMapper.insert(transaction3);

        List<Transaction> results = transactionMapper.search(testUserId, null, null, "100.00", "200.00", null, null, 0, 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(results.get(0).getAmount()));
    }

    @Test
    void countSearch_shouldReturnCorrectCount() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        transaction1.setDescription("测试关键词");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transaction2.setDescription("其他描述");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        Long count = transactionMapper.countSearch(testUserId, "测试", null, null, null, null, null);

        assertNotNull(count);
        assertEquals(1, count);
    }

    @Test
    void updateRefundStatus_shouldSuccess() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        int result = transactionMapper.updateRefundStatus(transaction.getId(), "partial", "50.00");

        assertEquals(1, result);

        Transaction updated = transactionMapper.selectById(transaction.getId());
        assertEquals("partial", updated.getRefundStatus());
        assertEquals(0, new BigDecimal("50.00").compareTo(updated.getRefundedAmount()));
    }

    @Test
    void updateRefundStatus_shouldReturnZeroWhenNotExist() {
        int result = transactionMapper.updateRefundStatus(99999L, "full", "100.00");

        assertEquals(0, result);
    }

    @Test
    void searchTransactions_shouldReturnWithAdvancedQuery() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        transaction1.setDescription("支出测试1");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transaction2.setDescription("支出测试2");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        TransactionSearchQuery query = new TransactionSearchQuery();
        query.setKeyword("支出");
        query.setPage(1);
        query.setPageSize(10);

        List<Transaction> results = transactionMapper.searchTransactions(testUserId, query);

        assertNotNull(results);
        assertTrue(results.size() >= 2);
    }

    @Test
    void countSearchTransactions_shouldReturnCorrectCount() {
        Transaction transaction1 = createTransaction("100.00", "2024-01-01");
        transaction1.setDescription("高级搜索测试");
        Transaction transaction2 = createTransaction("200.00", "2024-01-02");
        transaction2.setDescription("其他交易");
        transactionMapper.insert(transaction1);
        transactionMapper.insert(transaction2);

        TransactionSearchQuery query = new TransactionSearchQuery();
        query.setKeyword("高级搜索");
        query.setPage(1);
        query.setPageSize(10);

        Long count = transactionMapper.countSearchTransactions(testUserId, query);

        assertNotNull(count);
        assertEquals(1, count);
    }

    @Test
    void countByCategoryId_shouldReturnCorrectCount() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transaction.setTransactionType("expense");
        transactionMapper.insert(transaction);

        Long count = transactionMapper.countByCategoryId(testCategoryId);

        assertNotNull(count);
    }

    @Test
    void updateCategoryId_shouldBatchUpdate() {
        Long oldCategoryId = testCategoryId;
        Category newCategory = new Category();
        newCategory.setUserId(testUserId);
        newCategory.setName("新分类_" + System.currentTimeMillis());
        newCategory.setType("expense");
        newCategory.setIcon("🍕");
        newCategory.setAlertThreshold(new BigDecimal("0.8"));
        newCategory.setSortOrder(1);
        newCategory.setIsDeleted(false);
        newCategory.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(newCategory);
        Long newCategoryId = newCategory.getId();

        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        int result = transactionMapper.updateCategoryId(oldCategoryId, newCategoryId);

        assertTrue(result >= 0);
    }

    @Test
    void selectByAccountId_shouldReturnTransactions() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        List<Transaction> results = transactionMapper.selectByAccountId(testAccountId, 0, 10);

        assertNotNull(results);
    }

    @Test
    void countByAccountId_shouldReturnCorrectCount() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        Long count = transactionMapper.countByAccountId(testAccountId);

        assertNotNull(count);
    }

    @Test
    void selectRecentByAccountId_shouldReturnRecentTransactions() {
        Transaction transaction = createTransaction("100.00", "2024-01-01");
        transactionMapper.insert(transaction);

        List<Transaction> results = transactionMapper.selectRecentByAccountId(testAccountId, 10);

        assertNotNull(results);
    }
}