package com.duoduocode.service.transaction.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.transaction.dto.*;
import com.duoduocode.service.transaction.entity.Entry;
import com.duoduocode.service.transaction.mapper.EntryMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

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
        user.setOpenid("test_openid_transaction_service_" + System.currentTimeMillis());
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

    private TransactionDTO createExpenseDTO(BigDecimal amount) {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(amount);
        dto.setAccountId(testAccountId);
        dto.setCategoryId(testCategoryId);
        dto.setTransactionType("expense");
        dto.setDescription("测试支出_" + System.currentTimeMillis());
        return dto;
    }

    private TransactionDTO createIncomeDTO(BigDecimal amount) {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(amount);
        dto.setAccountId(testAccountId);
        dto.setCategoryId(testCategoryId);
        dto.setTransactionType("income");
        dto.setDescription("测试收入_" + System.currentTimeMillis());
        return dto;
    }

    @Test
    void getTransactionList_shouldReturnPageResult() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        transactionService.createTransaction(testUserId, dto);

        TransactionQuery query = new TransactionQuery();
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.getTransactionList(testUserId, query);

        assertNotNull(result);
        assertNotNull(result.getList());
        assertTrue(result.getTotal() >= 1);
    }

    @Test
    void getTransactionList_shouldFilterByDateRange() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        transactionService.createTransaction(testUserId, dto);

        TransactionQuery query = new TransactionQuery();
        query.setPage(1);
        query.setPageSize(10);
        query.setStartDate(LocalDate.now().minusDays(1).toString());
        query.setEndDate(LocalDate.now().plusDays(1).toString());

        PageResult<TransactionVO> result = transactionService.getTransactionList(testUserId, query);

        assertNotNull(result);
    }

    @Test
    void getTransactionList_shouldReturnEmptyWhenNoData() {
        TransactionQuery query = new TransactionQuery();
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.getTransactionList(testUserId, query);

        assertNotNull(result);
        assertNotNull(result.getList());
    }

    @Test
    void getRecentTransactions_shouldReturnRecentTransactions() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        transactionService.createTransaction(testUserId, dto);

        List<TransactionVO> result = transactionService.getRecentTransactions(testUserId, 10);

        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    void getRecentTransactions_shouldRespectLimit() {
        for (int i = 0; i < 5; i++) {
            TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
            transactionService.createTransaction(testUserId, dto);
        }

        List<TransactionVO> result = transactionService.getRecentTransactions(testUserId, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void getTransactionDetail_shouldReturnTransaction() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        TransactionVO result = transactionService.getTransactionDetail(transactionId);

        assertNotNull(result);
        assertEquals(transactionId, result.getId());
        assertNotNull(result.getEntries());
    }

    @Test
    void getTransactionDetail_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionDetail(99999L);
        });
    }

    @Test
    void getTransactionDetail_shouldThrowExceptionWhenDeleted() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        Long transactionId = transactionService.createTransaction(testUserId, dto);
        transactionService.deleteTransaction(transactionId);

        assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionDetail(transactionId);
        });
    }

    @Test
    void createTransaction_expense_shouldSuccess() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        assertNotNull(transactionId);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("expense", vo.getTransactionType());
        assertEquals(0, new BigDecimal("100.00").compareTo(vo.getAmount()));
        assertEquals(2, vo.getEntries().size());
    }

    @Test
    void createTransaction_income_shouldSuccess() {
        TransactionDTO dto = createIncomeDTO(new BigDecimal("200.00"));
        dto.setDate(LocalDate.now());

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        assertNotNull(transactionId);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("income", vo.getTransactionType());
    }

    @Test
    void createTransaction_transfer_shouldSuccess() {
        Account targetAccount = new Account();
        targetAccount.setUserId(testUserId);
        targetAccount.setName("目标账户_" + System.currentTimeMillis());
        targetAccount.setType("asset");
        targetAccount.setIcon("💳");
        targetAccount.setColor("#FF6347");
        targetAccount.setInitialBalance(BigDecimal.ZERO);
        targetAccount.setCreditLimit(BigDecimal.ZERO);
        targetAccount.setIncludeInNetWorth(true);
        targetAccount.setAllowTransfer(true);
        targetAccount.setEnableAlert(false);
        targetAccount.setAlertThreshold(new BigDecimal("0.8"));
        targetAccount.setSortOrder(1);
        targetAccount.setIsDeleted(false);
        targetAccount.setCreatedAt(LocalDateTime.now());
        targetAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(targetAccount);

        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("300.00"));
        dto.setAccountId(testAccountId);
        dto.setTargetAccountId(targetAccount.getId());
        dto.setTransactionType("transfer");
        dto.setDescription("测试转账_" + System.currentTimeMillis());
        dto.setDate(LocalDate.now());

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        assertNotNull(transactionId);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("transfer", vo.getTransactionType());
        assertEquals(2, vo.getEntries().size());
    }

    @Test
    void createTransaction_fullMode_shouldSuccess() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setDescription("完整模式测试_" + System.currentTimeMillis());
        dto.setMode("full");
        dto.setDate(LocalDate.now());

        List<EntryDTO> entries = new ArrayList<>();
        EntryDTO entry1 = new EntryDTO();
        entry1.setAccountId(testAccountId);
        entry1.setDebit(new BigDecimal("500.00"));
        entry1.setAccountType("account");
        entries.add(entry1);

        EntryDTO entry2 = new EntryDTO();
        entry2.setAccountId(testCategoryId);
        entry2.setCredit(new BigDecimal("500.00"));
        entry2.setAccountType("category");
        entries.add(entry2);

        dto.setEntries(entries);

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        assertNotNull(transactionId);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("full", vo.getMode());
        assertEquals(2, vo.getEntries().size());
    }

    @Test
    void createTransaction_fullMode_shouldThrowWhenNotBalanced() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setDescription("不平衡测试_" + System.currentTimeMillis());
        dto.setMode("full");
        dto.setDate(LocalDate.now());

        List<EntryDTO> entries = new ArrayList<>();
        EntryDTO entry1 = new EntryDTO();
        entry1.setAccountId(testAccountId);
        entry1.setDebit(new BigDecimal("500.00"));
        entry1.setAccountType("account");
        entries.add(entry1);

        EntryDTO entry2 = new EntryDTO();
        entry2.setAccountId(testCategoryId);
        entry2.setCredit(new BigDecimal("300.00"));
        entry2.setAccountType("category");
        entries.add(entry2);

        dto.setEntries(entries);

        assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(testUserId, dto);
        });
    }

    @Test
    void createTransaction_simpleMode_shouldThrowWhenTypeMissing() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setAccountId(testAccountId);
        dto.setCategoryId(testCategoryId);
        dto.setTransactionType(null);
        dto.setDescription("测试_" + System.currentTimeMillis());

        assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(testUserId, dto);
        });
    }

    @Test
    void createTransaction_shouldThrowWhenAmountInvalid() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(BigDecimal.ZERO);
        dto.setAccountId(testAccountId);
        dto.setCategoryId(testCategoryId);
        dto.setTransactionType("expense");

        assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(testUserId, dto);
        });
    }

    @Test
    void updateTransaction_shouldSuccess() {
        TransactionDTO createDto = createExpenseDTO(new BigDecimal("100.00"));
        createDto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, createDto);

        TransactionDTO updateDto = new TransactionDTO();
        updateDto.setAmount(new BigDecimal("200.00"));
        updateDto.setDescription("更新后的描述_" + System.currentTimeMillis());
        updateDto.setTransactionType("expense");
        updateDto.setAccountId(testAccountId);
        updateDto.setCategoryId(testCategoryId);

        transactionService.updateTransaction(transactionId, updateDto);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals(0, new BigDecimal("200.00").compareTo(vo.getAmount()));
        assertTrue(vo.getDescription().contains("更新后的描述"));
    }

    @Test
    void updateTransaction_shouldThrowWhenNotExist() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));

        assertThrows(BusinessException.class, () -> {
            transactionService.updateTransaction(99999L, dto);
        });
    }

    @Test
    void updateTransaction_shouldThrowWhenRefunded() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("50.00"));
        transactionService.refundTransaction(transactionId, refundDto);

        TransactionDTO updateDto = new TransactionDTO();
        updateDto.setAmount(new BigDecimal("200.00"));
        updateDto.setTransactionType("expense");
        updateDto.setAccountId(testAccountId);
        updateDto.setCategoryId(testCategoryId);

        assertThrows(BusinessException.class, () -> {
            transactionService.updateTransaction(transactionId, updateDto);
        });
    }

    @Test
    void deleteTransaction_shouldSuccess() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        transactionService.deleteTransaction(transactionId);

        assertThrows(BusinessException.class, () -> {
            transactionService.getTransactionDetail(transactionId);
        });
    }

    @Test
    void deleteTransaction_shouldThrowWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            transactionService.deleteTransaction(99999L);
        });
    }

    @Test
    void refundTransaction_partial_shouldSuccess() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("30.00"));

        transactionService.refundTransaction(transactionId, refundDto);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("partial", vo.getRefundStatus());
        assertEquals(0, new BigDecimal("30.00").compareTo(vo.getRefundedAmount()));
    }

    @Test
    void refundTransaction_full_shouldSuccess() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("100.00"));

        transactionService.refundTransaction(transactionId, refundDto);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("full", vo.getRefundStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(vo.getRefundedAmount()));
    }

    @Test
    void refundTransaction_shouldThrowWhenNotExist() {
        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("50.00"));

        assertThrows(BusinessException.class, () -> {
            transactionService.refundTransaction(99999L, refundDto);
        });
    }

    @Test
    void refundTransaction_shouldThrowWhenAmountExceedsOriginal() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("150.00"));

        assertThrows(BusinessException.class, () -> {
            transactionService.refundTransaction(transactionId, refundDto);
        });
    }

    @Test
    void refundTransaction_shouldThrowWhenAlreadyFullyRefunded() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(new BigDecimal("100.00"));
        transactionService.refundTransaction(transactionId, refundDto);

        RefundDTO secondRefund = new RefundDTO();
        secondRefund.setAmount(new BigDecimal("50.00"));

        assertThrows(BusinessException.class, () -> {
            transactionService.refundTransaction(transactionId, secondRefund);
        });
    }

    @Test
    void refundTransaction_shouldThrowWhenAmountInvalid() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        Long transactionId = transactionService.createTransaction(testUserId, dto);

        RefundDTO refundDto = new RefundDTO();
        refundDto.setAmount(BigDecimal.ZERO);

        assertThrows(BusinessException.class, () -> {
            transactionService.refundTransaction(transactionId, refundDto);
        });
    }

    @Test
    void checkDuplicate_shouldReturnFalseWhenNoDuplicate() {
        DuplicateCheckDTO dto = new DuplicateCheckDTO();
        dto.setAmount(new BigDecimal("999.00"));
        dto.setDate(LocalDate.now().toString());

        Map<String, Object> result = transactionService.checkDuplicate(testUserId, dto);

        assertNotNull(result);
        assertEquals(false, result.get("isDuplicate"));
        assertNull(result.get("existingTransaction"));
    }

    @Test
    void checkDuplicate_shouldReturnTrueWhenDuplicateExists() {
        TransactionDTO createDto = createExpenseDTO(new BigDecimal("100.00"));
        createDto.setDate(LocalDate.now());
        createDto.setDescription("重复检测测试");
        transactionService.createTransaction(testUserId, createDto);

        DuplicateCheckDTO dto = new DuplicateCheckDTO();
        dto.setAmount(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now().toString());
        dto.setDescription("重复检测测试");

        Map<String, Object> result = transactionService.checkDuplicate(testUserId, dto);

        assertNotNull(result);
        assertEquals(true, result.get("isDuplicate"));
        assertNotNull(result.get("existingTransaction"));
    }

    @Test
    void searchTransactions_old_shouldReturnResults() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        dto.setDescription("搜索测试关键字");
        transactionService.createTransaction(testUserId, dto);

        SearchQuery query = new SearchQuery();
        query.setKeyword("搜索测试");
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
        assertNotNull(result.getList());
    }

    @Test
    void searchTransactions_old_shouldFilterByAccountId() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        transactionService.createTransaction(testUserId, dto);

        SearchQuery query = new SearchQuery();
        query.setAccountId(testAccountId);
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
    }

    @Test
    void searchTransactions_old_shouldFilterByAmountRange() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("150.00"));
        dto.setDate(LocalDate.now());
        transactionService.createTransaction(testUserId, dto);

        SearchQuery query = new SearchQuery();
        query.setMinAmount("100.00");
        query.setMaxAmount("200.00");
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
    }

    @Test
    void searchTransactions_new_shouldReturnResults() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        dto.setDescription("高级搜索测试关键字");
        transactionService.createTransaction(testUserId, dto);

        TransactionSearchQuery query = new TransactionSearchQuery();
        query.setKeyword("高级搜索");
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
        assertNotNull(result.getList());
    }

    @Test
    void searchTransactions_new_shouldFilterByDateRange() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        transactionService.createTransaction(testUserId, dto);

        TransactionSearchQuery query = new TransactionSearchQuery();
        query.setStartDate(LocalDate.now().minusDays(1).toString());
        query.setEndDate(LocalDate.now().plusDays(1).toString());
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
    }

    @Test
    void searchTransactions_new_shouldFilterByAmountRange() {
        TransactionDTO dto = createExpenseDTO(new BigDecimal("100.00"));
        dto.setDate(LocalDate.now());
        transactionService.createTransaction(testUserId, dto);

        TransactionSearchQuery query = new TransactionSearchQuery();
        query.setMinAmount(new BigDecimal("50.00"));
        query.setMaxAmount(new BigDecimal("200.00"));
        query.setPage(1);
        query.setPageSize(10);

        PageResult<TransactionVO> result = transactionService.searchTransactions(testUserId, query);

        assertNotNull(result);
    }

    @Test
    void createTransaction_transfer_shouldThrowWhenSameAccount() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("300.00"));
        dto.setAccountId(testAccountId);
        dto.setTargetAccountId(testAccountId);
        dto.setTransactionType("transfer");
        dto.setDescription("同一账户转账_" + System.currentTimeMillis());
        dto.setDate(LocalDate.now());

        assertThrows(BusinessException.class, () -> {
            transactionService.createTransaction(testUserId, dto);
        });
    }

    @Test
    void createTransaction_repayment_shouldSuccess() {
        Category liabilityCategory = new Category();
        liabilityCategory.setUserId(testUserId);
        liabilityCategory.setName("负债分类_" + System.currentTimeMillis());
        liabilityCategory.setType("expense");
        liabilityCategory.setIcon("💳");
        liabilityCategory.setSortOrder(1);
        liabilityCategory.setIsDeleted(false);
        liabilityCategory.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(liabilityCategory);

        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setAccountId(testAccountId);
        dto.setCategoryId(liabilityCategory.getId());
        dto.setTransactionType("repayment");
        dto.setDescription("测试还款_" + System.currentTimeMillis());
        dto.setDate(LocalDate.now());

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        assertNotNull(transactionId);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals("repayment", vo.getTransactionType());
    }

    @Test
    void updateTransaction_fullMode_shouldRegenerateEntries() {
        TransactionDTO dto = new TransactionDTO();
        dto.setAmount(new BigDecimal("500.00"));
        dto.setDescription("完整模式更新测试_" + System.currentTimeMillis());
        dto.setMode("full");
        dto.setDate(LocalDate.now());

        List<EntryDTO> entries = new ArrayList<>();
        EntryDTO entry1 = new EntryDTO();
        entry1.setAccountId(testAccountId);
        entry1.setDebit(new BigDecimal("500.00"));
        entry1.setAccountType("account");
        entries.add(entry1);

        EntryDTO entry2 = new EntryDTO();
        entry2.setAccountId(testCategoryId);
        entry2.setCredit(new BigDecimal("500.00"));
        entry2.setAccountType("category");
        entries.add(entry2);

        dto.setEntries(entries);

        Long transactionId = transactionService.createTransaction(testUserId, dto);

        TransactionDTO updateDto = new TransactionDTO();
        updateDto.setAmount(new BigDecimal("600.00"));
        updateDto.setDescription("更新后_" + System.currentTimeMillis());
        updateDto.setMode("full");
        updateDto.setDate(LocalDate.now());

        List<EntryDTO> newEntries = new ArrayList<>();
        EntryDTO newEntry1 = new EntryDTO();
        newEntry1.setAccountId(testAccountId);
        newEntry1.setDebit(new BigDecimal("600.00"));
        newEntry1.setAccountType("account");
        newEntries.add(newEntry1);

        EntryDTO newEntry2 = new EntryDTO();
        newEntry2.setAccountId(testCategoryId);
        newEntry2.setCredit(new BigDecimal("600.00"));
        newEntry2.setAccountType("category");
        newEntries.add(newEntry2);

        updateDto.setEntries(newEntries);

        transactionService.updateTransaction(transactionId, updateDto);

        TransactionVO vo = transactionService.getTransactionDetail(transactionId);
        assertEquals(0, new BigDecimal("600.00").compareTo(vo.getAmount()));
    }
}