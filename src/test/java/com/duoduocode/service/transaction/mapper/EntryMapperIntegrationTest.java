package com.duoduocode.service.transaction.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.transaction.entity.Entry;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EntryMapperIntegrationTest {

    @Autowired
    private EntryMapper entryMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private Long testUserId;
    private Long testAccountId;
    private Long testCategoryId;
    private Long testTransactionId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_entry_mapper_" + System.currentTimeMillis());
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

        Transaction transaction = new Transaction();
        transaction.setUserId(testUserId);
        transaction.setDate(LocalDate.now());
        transaction.setTime(LocalTime.now());
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setDescription("测试交易_" + System.currentTimeMillis());
        transaction.setMode("simple");
        transaction.setTransactionType("expense");
        transaction.setRefundStatus("none");
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setIsDeleted(0);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionMapper.insert(transaction);
        testTransactionId = transaction.getId();
    }

    private Entry createEntry(Long transactionId, Long accountId, BigDecimal debit, BigDecimal credit, String accountType) {
        Entry entry = new Entry();
        entry.setTransactionId(transactionId);
        entry.setAccountId(accountId);
        entry.setDebit(debit);
        entry.setCredit(credit);
        entry.setAccountType(accountType);
        entry.setIsDeleted(0);
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }

    @Test
    void insert_shouldSuccess() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");

        int result = entryMapper.insert(entry);

        assertEquals(1, result);
        assertNotNull(entry.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        entry.setIsDeleted(0);

        entryMapper.insert(entry);

        Entry saved = entryMapper.selectById(entry.getId());
        assertNotNull(saved);
        assertEquals(testTransactionId, saved.getTransactionId());
        assertEquals(testAccountId, saved.getAccountId());
        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getDebit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getCredit())); // COALESCE returns 0 for null
        assertEquals("account", saved.getAccountType());
        assertEquals(0, saved.getIsDeleted());
    }

    @Test
    void selectById_shouldReturnEntry() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        entryMapper.insert(entry);

        Entry result = entryMapper.selectById(entry.getId());

        assertNotNull(result);
        assertEquals(entry.getId(), result.getId());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getDebit()));
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        Entry result = entryMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByTransactionId_shouldReturnEntries() {
        Entry entry1 = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        Entry entry2 = createEntry(testTransactionId, testCategoryId, null, new BigDecimal("100.00"), "category");
        entryMapper.insert(entry1);
        entryMapper.insert(entry2);

        List<Entry> results = entryMapper.selectByTransactionId(testTransactionId);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void selectByTransactionId_shouldReturnEmptyListWhenNoEntries() {
        List<Entry> results = entryMapper.selectByTransactionId(testTransactionId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void selectByAccountId_shouldReturnEntries() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        entryMapper.insert(entry);

        List<Entry> results = entryMapper.selectByAccountId(testAccountId);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
    }

    @Test
    void selectByAccountId_shouldReturnEmptyListWhenNoEntries() {
        List<Entry> results = entryMapper.selectByAccountId(99999L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void sumDebitByCategoryIdAndDateRange_shouldReturnSum() {
        Entry entry1 = createEntry(testTransactionId, testCategoryId, new BigDecimal("100.00"), null, "category");
        entryMapper.insert(entry1);

        BigDecimal sum = entryMapper.sumDebitByCategoryIdAndDateRange(
                testCategoryId,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("100.00").compareTo(sum));
    }

    @Test
    void sumDebitByCategoryIdAndDateRange_shouldReturnZeroWhenNoEntries() {
        BigDecimal sum = entryMapper.sumDebitByCategoryIdAndDateRange(
                99999L,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertNotNull(sum);
        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }

    @Test
    void sumCreditByCategoryIdAndDateRange_shouldReturnSum() {
        Entry entry = createEntry(testTransactionId, testCategoryId, null, new BigDecimal("200.00"), "category");
        entryMapper.insert(entry);

        BigDecimal sum = entryMapper.sumCreditByCategoryIdAndDateRange(
                testCategoryId,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("200.00").compareTo(sum));
    }

    @Test
    void sumDebitByTransactionId_shouldReturnSum() {
        Entry entry1 = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        Entry entry2 = createEntry(testTransactionId, testCategoryId, new BigDecimal("50.00"), null, "category");
        entryMapper.insert(entry1);
        entryMapper.insert(entry2);

        BigDecimal sum = entryMapper.sumDebitByTransactionId(testTransactionId);

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("150.00").compareTo(sum));
    }

    @Test
    void sumDebitByTransactionId_shouldReturnZeroWhenNoEntries() {
        BigDecimal sum = entryMapper.sumDebitByTransactionId(99999L);

        assertNotNull(sum);
        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }

    @Test
    void sumCreditByTransactionId_shouldReturnSum() {
        Entry entry1 = createEntry(testTransactionId, testAccountId, null, new BigDecimal("100.00"), "account");
        Entry entry2 = createEntry(testTransactionId, testCategoryId, null, new BigDecimal("50.00"), "category");
        entryMapper.insert(entry1);
        entryMapper.insert(entry2);

        BigDecimal sum = entryMapper.sumCreditByTransactionId(testTransactionId);

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("150.00").compareTo(sum));
    }

    @Test
    void sumDebitByAccountId_shouldReturnSum() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("300.00"), null, "account");
        entryMapper.insert(entry);

        BigDecimal sum = entryMapper.sumDebitByAccountId(testAccountId);

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("300.00").compareTo(sum));
    }

    @Test
    void sumCreditByAccountId_shouldReturnSum() {
        Entry entry = createEntry(testTransactionId, testAccountId, null, new BigDecimal("400.00"), "account");
        entryMapper.insert(entry);

        BigDecimal sum = entryMapper.sumCreditByAccountId(testAccountId);

        assertNotNull(sum);
        assertEquals(0, new BigDecimal("400.00").compareTo(sum));
    }

    @Test
    void deleteById_shouldSuccess() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        entryMapper.insert(entry);

        int result = entryMapper.deleteById(entry.getId());

        assertEquals(1, result);

        Entry deleted = entryMapper.selectById(entry.getId());
        assertNull(deleted);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = entryMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void batchInsert_shouldInsertMultipleEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account"));
        entries.add(createEntry(testTransactionId, testCategoryId, null, new BigDecimal("100.00"), "category"));

        int result = entryMapper.batchInsert(entries);

        assertEquals(2, result);
        // 验证插入是否成功
        List<Entry> savedEntries = entryMapper.selectByTransactionId(testTransactionId);
        assertEquals(2, savedEntries.size());
    }

    @Test
    void batchInsert_shouldInsertSingleEntry() {
        List<Entry> entries = new ArrayList<>();
        entries.add(createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account"));

        int result = entryMapper.batchInsert(entries);

        assertEquals(1, result);
    }

    @Test
    void deleteByTransactionId_shouldDeleteAllEntries() {
        Entry entry1 = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        Entry entry2 = createEntry(testTransactionId, testCategoryId, null, new BigDecimal("100.00"), "category");
        entryMapper.insert(entry1);
        entryMapper.insert(entry2);

        int result = entryMapper.deleteByTransactionId(testTransactionId);

        assertEquals(2, result);

        List<Entry> remaining = entryMapper.selectByTransactionId(testTransactionId);
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteByTransactionId_shouldReturnZeroWhenNoEntries() {
        int result = entryMapper.deleteByTransactionId(99999L);

        assertEquals(0, result);
    }

    @Test
    void selectByTransactionId_shouldNotReturnDeletedEntries() {
        Entry entry = createEntry(testTransactionId, testAccountId, new BigDecimal("100.00"), null, "account");
        entry.setIsDeleted(1);
        entryMapper.insert(entry);

        List<Entry> results = entryMapper.selectByTransactionId(testTransactionId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void sumDebitByCategoryIdAndDateRange_shouldExcludeDeletedEntries() {
        Entry entry = createEntry(testTransactionId, testCategoryId, new BigDecimal("100.00"), null, "category");
        entry.setIsDeleted(1);
        entryMapper.insert(entry);

        BigDecimal sum = entryMapper.sumDebitByCategoryIdAndDateRange(
                testCategoryId,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        assertNotNull(sum);
        assertEquals(0, BigDecimal.ZERO.compareTo(sum));
    }
}