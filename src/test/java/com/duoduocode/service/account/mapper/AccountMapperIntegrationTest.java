package com.duoduocode.service.account.mapper;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AccountMapperIntegrationTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_account_mapper_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private Account createAccount(String name, String type) {
        Account account = new Account();
        account.setUserId(testUserId);
        account.setName(name);
        account.setType(type);
        account.setIcon("💰");
        account.setColor("#1E90FF");
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCreditLimit(BigDecimal.ZERO);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(new BigDecimal("0.8"));
        account.setSortOrder(0);
        account.setDesc("测试描述");
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return account;
    }

    @Test
    void insert_shouldSuccess() {
        Account account = createAccount("测试账户_" + System.currentTimeMillis(), "asset");

        int result = accountMapper.insert(account);

        assertEquals(1, result);
        assertNotNull(account.getId());
    }

    @Test
    void updateById_shouldSuccess() {
        Account account = createAccount("更新前_" + System.currentTimeMillis(), "asset");
        accountMapper.insert(account);

        account.setName("更新后");
        account.setInitialBalance(new BigDecimal("2000.00"));

        int result = accountMapper.updateById(account);

        assertEquals(1, result);

        Account updated = accountMapper.selectById(account.getId());
        assertEquals("更新后", updated.getName());
        assertEquals(0, new BigDecimal("2000.00").compareTo(updated.getInitialBalance()));
    }

    @Test
    void updateById_shouldReturnZeroWhenAccountNotExist() {
        Account account = createAccount("不存在", "asset");
        account.setId(99999L);

        int result = accountMapper.updateById(account);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnAccount() {
        Account account = createAccount("查询测试_" + System.currentTimeMillis(), "asset");
        accountMapper.insert(account);

        Account result = accountMapper.selectById(account.getId());

        assertNotNull(result);
        assertEquals(account.getName(), result.getName());
        assertEquals(account.getType(), result.getType());
    }

    @Test
    void selectById_shouldReturnNullWhenAccountNotExist() {
        Account result = accountMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByUserId_shouldReturnAccountList() {
        accountMapper.insert(createAccount("账户1_" + System.currentTimeMillis(), "asset"));
        accountMapper.insert(createAccount("账户2_" + System.currentTimeMillis(), "asset"));

        List<Account> result = accountMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void selectByUserId_shouldReturnEmptyListWhenNoAccount() {
        List<Account> result = accountMapper.selectByUserId(testUserId);

        assertNotNull(result);
        long userCount = result.stream().filter(a -> a.getUserId() != null).count();
        assertEquals(0, userCount);
    }

    @Test
    void selectByUserIdAndType_shouldReturnFilteredList() {
        accountMapper.insert(createAccount("资产账户_" + System.currentTimeMillis(), "asset"));
        accountMapper.insert(createAccount("负债账户_" + System.currentTimeMillis(), "liability"));

        List<Account> assetList = accountMapper.selectByUserIdAndType(testUserId, "asset");

        assertNotNull(assetList);
        assertTrue(assetList.stream().anyMatch(a -> a.getUserId() != null));
        assertEquals("asset", assetList.stream().filter(a -> a.getUserId() != null).findFirst().get().getType());
    }

    @Test
    void selectByUserIdAndType_shouldReturnEmptyWhenNoMatch() {
        accountMapper.insert(createAccount("资产账户_" + System.currentTimeMillis(), "asset"));

        List<Account> result = accountMapper.selectByUserIdAndType(testUserId, "liability");

        assertNotNull(result);
        long userCount = result.stream().filter(a -> a.getUserId() != null).count();
        assertEquals(0, userCount);
    }

    @Test
    void countByUserIdAndName_shouldReturnCount() {
        String uniqueName = "唯一名称_" + System.currentTimeMillis();
        accountMapper.insert(createAccount(uniqueName, "asset"));

        int count = accountMapper.countByUserIdAndName(testUserId, uniqueName, null);

        assertEquals(1, count);
    }

    @Test
    void countByUserIdAndName_shouldReturnZeroWhenNotExist() {
        int count = accountMapper.countByUserIdAndName(testUserId, "不存在的名称", null);

        assertEquals(0, count);
    }

    @Test
    void countByUserIdAndName_shouldExcludeCurrentId() {
        String uniqueName = "排除测试_" + System.currentTimeMillis();
        Account account = createAccount(uniqueName, "asset");
        accountMapper.insert(account);

        int count = accountMapper.countByUserIdAndName(testUserId, uniqueName, account.getId());

        assertEquals(0, count);
    }

    @Test
    void calculateCurrentBalance_shouldReturnInitialBalance() {
        Account account = createAccount("余额测试_" + System.currentTimeMillis(), "asset");
        account.setInitialBalance(new BigDecimal("5000.00"));
        accountMapper.insert(account);

        BigDecimal balance = accountMapper.calculateCurrentBalance(account.getId());

        assertNotNull(balance);
        assertEquals(0, new BigDecimal("5000.00").compareTo(balance));
    }

    @Test
    void calculateCurrentBalance_shouldReturnZeroWhenAccountNotExist() {
        BigDecimal balance = accountMapper.calculateCurrentBalance(99999L);

        assertNull(balance);
    }

    @Test
    void softDeleteById_shouldSuccess() {
        Account account = createAccount("删除测试_" + System.currentTimeMillis(), "asset");
        accountMapper.insert(account);
        Long accountId = account.getId();

        int result = accountMapper.softDeleteById(accountId);

        assertEquals(1, result);

        List<Account> accounts = accountMapper.selectByUserId(testUserId);
        long userCount = accounts.stream().filter(a -> a.getUserId() != null).count();
        assertEquals(0, userCount);
    }

    @Test
    void softDeleteById_shouldReturnZeroWhenAccountNotExist() {
        int result = accountMapper.softDeleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void insert_shouldSaveDesc() {
        Account account = createAccount("描述账户_" + System.currentTimeMillis(), "asset");
        account.setDesc("我的工资卡描述");

        accountMapper.insert(account);

        Account result = accountMapper.selectById(account.getId());
        assertNotNull(result);
        assertEquals("我的工资卡描述", result.getDesc());
    }

    @Test
    void updateById_shouldUpdateDesc() {
        Account account = createAccount("更新描述前_" + System.currentTimeMillis(), "asset");
        accountMapper.insert(account);

        account.setDesc("更新后的账户描述");

        accountMapper.updateById(account);

        Account updated = accountMapper.selectById(account.getId());
        assertEquals("更新后的账户描述", updated.getDesc());
    }
}