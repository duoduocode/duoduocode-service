package com.duoduocode.service.account.service;

import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.transaction.dto.TransactionVO;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}