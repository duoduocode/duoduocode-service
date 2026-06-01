package com.duoduocode.service.account.service;

import com.duoduocode.service.account.dto.AccountStatisticsVO;
import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.common.entity.UserDataHide;
import com.duoduocode.service.common.mapper.UserDataHideMapper;
import com.duoduocode.service.transaction.dto.TransactionVO;
import com.duoduocode.service.transaction.entity.Transaction;
import com.duoduocode.service.transaction.entity.Entry;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账户服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final UserDataHideMapper userDataHideMapper;
    private final EntryMapper entryMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 获取账户列表
     */
    public Map<String, Object> getAccountList(Long userId) {
        List<Account> accounts = accountMapper.selectByUserId(userId);

        // 按类型分组
        Map<String, List<Map<String, Object>>> grouped = new HashMap<>();
        grouped.put("asset", new ArrayList<>());
        grouped.put("liability", new ArrayList<>());
        grouped.put("investment", new ArrayList<>());

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalInvestments = BigDecimal.ZERO;

        for (Account account : accounts) {
            Map<String, Object> accountMap = convertToMap(account);
            String type = account.getType();
            BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;

            if ("asset".equals(type)) {
                grouped.get("asset").add(accountMap);
                totalAssets = totalAssets.add(balance);
            } else if ("liability".equals(type)) {
                grouped.get("liability").add(accountMap);
                totalLiabilities = totalLiabilities.add(balance);
            } else if ("investment".equals(type)) {
                grouped.get("investment").add(accountMap);
                totalInvestments = totalInvestments.add(balance);
            }
        }

        // 计算净资产
        BigDecimal netWorth = totalAssets.add(totalInvestments).subtract(totalLiabilities);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAssets", totalAssets);
        summary.put("totalLiabilities", totalLiabilities);
        summary.put("totalInvestments", totalInvestments);
        summary.put("netWorth", netWorth);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("accounts", grouped);

        return result;
    }

    /**
     * 获取账户详情
     */
    public Map<String, Object> getAccountDetail(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        Map<String, Object> result = convertToMap(account);

        // 获取最近10条交易
        List<Transaction> transactions = transactionMapper.selectRecentByAccountId(accountId, 10);
        List<Map<String, Object>> recentTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            Map<String, Object> txMap = new HashMap<>();
            txMap.put("id", t.getId());
            txMap.put("amount", t.getAmount());
            txMap.put("date", t.getDate());
            txMap.put("description", t.getDescription());
            recentTransactions.add(txMap);
        }
        result.put("recentTransactions", recentTransactions);

        return result;
    }

    /**
     * 创建账户
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(Long userId, Map<String, Object> dto) {
        String name = (String) dto.get("name");
        String type = (String) dto.get("type");

        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账户名称不能为空");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "账户类型不能为空");
        }

        Account account = new Account();
        account.setUserId(userId);
        account.setName(name.trim());
        account.setType(type);
        account.setIcon((String) dto.getOrDefault("icon", "💰"));
        account.setColor((String) dto.getOrDefault("color", "#07C160"));

        Object initialBalance = dto.get("initialBalance");
        account.setInitialBalance(initialBalance != null ? new BigDecimal(initialBalance.toString()) : BigDecimal.ZERO);

        Object creditLimit = dto.get("creditLimit");
        account.setCreditLimit(creditLimit != null ? new BigDecimal(creditLimit.toString()) : null);

        account.setIncludeInNetWorth((Boolean) dto.getOrDefault("includeInNetWorth", true));
        account.setAllowTransfer((Boolean) dto.getOrDefault("allowTransfer", true));
        account.setEnableAlert((Boolean) dto.getOrDefault("enableAlert", false));

        Object alertThreshold = dto.get("alertThreshold");
        account.setAlertThreshold(alertThreshold != null ? new BigDecimal(alertThreshold.toString()) : BigDecimal.ZERO);

        account.setSortOrder((Integer) dto.getOrDefault("sortOrder", 0));
        account.setDesc((String) dto.get("desc"));
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        accountMapper.insert(account);
        log.info("账户创建成功, accountId={}, name={}, type={}", account.getId(), name, type);
        return account.getId();
    }

    /**
     * 更新账户
     * 系统默认账户→"写时复制"：创建用户的副本并隐藏系统默认
     * 用户自有账户→直接更新
     *
     * @return 账户ID（系统默认归为复制后的新ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateAccount(Long userId, Long accountId, Map<String, Object> dto) {
        Account existing = accountMapper.selectById(accountId);
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        if (existing.getUserId() == null) {
            Account copy = new Account();
            copy.setUserId(userId);
            copy.setName(existing.getName());
            copy.setType(existing.getType());
            copy.setIcon(existing.getIcon());
            copy.setColor(existing.getColor());
            copy.setInitialBalance(existing.getInitialBalance());
            copy.setCreditLimit(existing.getCreditLimit());
            copy.setIncludeInNetWorth(existing.getIncludeInNetWorth());
            copy.setAllowTransfer(existing.getAllowTransfer());
            copy.setEnableAlert(existing.getEnableAlert());
            copy.setAlertThreshold(existing.getAlertThreshold());
            copy.setSortOrder(existing.getSortOrder());
            copy.setDesc(existing.getDesc());
            copy.setIsDeleted(false);
            copy.setCreatedAt(LocalDateTime.now());
            copy.setUpdatedAt(LocalDateTime.now());

            applyAccountDto(copy, dto, userId);

            accountMapper.insert(copy);

            UserDataHide hide = new UserDataHide();
            hide.setUserId(userId);
            hide.setDataType("account");
            hide.setRefId(accountId);
            userDataHideMapper.insert(hide);

            return copy.getId();
        }

        Account account = new Account();
        account.setId(accountId);
        account.setUpdatedAt(LocalDateTime.now());

        applyAccountDto(account, dto, existing.getUserId());

        accountMapper.updateById(account);
        return accountId;
    }

    private void applyAccountDto(Account account, Map<String, Object> dto, Long ownerUserId) {
        if (dto.containsKey("name")) {
            String name = (String) dto.get("name");
            if (name != null && !name.trim().isEmpty()) {
                account.setName(name.trim());
            }
        }

        if (dto.containsKey("type")) {
            account.setType((String) dto.get("type"));
        }
        if (dto.containsKey("icon")) {
            account.setIcon((String) dto.get("icon"));
        }
        if (dto.containsKey("color")) {
            account.setColor((String) dto.get("color"));
        }
        if (dto.containsKey("initialBalance")) {
            Object val = dto.get("initialBalance");
            account.setInitialBalance(val != null ? new BigDecimal(val.toString()) : null);
        }
        if (dto.containsKey("creditLimit")) {
            Object val = dto.get("creditLimit");
            account.setCreditLimit(val != null ? new BigDecimal(val.toString()) : null);
        }
        if (dto.containsKey("includeInNetWorth")) {
            account.setIncludeInNetWorth((Boolean) dto.get("includeInNetWorth"));
        }
        if (dto.containsKey("allowTransfer")) {
            account.setAllowTransfer((Boolean) dto.get("allowTransfer"));
        }
        if (dto.containsKey("enableAlert")) {
            account.setEnableAlert((Boolean) dto.get("enableAlert"));
        }
        if (dto.containsKey("alertThreshold")) {
            Object val = dto.get("alertThreshold");
            account.setAlertThreshold(val != null ? new BigDecimal(val.toString()) : null);
        }
        if (dto.containsKey("sortOrder")) {
            account.setSortOrder((Integer) dto.get("sortOrder"));
        }
        if (dto.containsKey("desc")) {
            account.setDesc((String) dto.get("desc"));
        }
    }

    /**
     * 删除账户
     * 系统默认账户：写入 user_data_hide 表隐藏
     * 用户自定义账户：软删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        if (account.getUserId() == null) {
            UserDataHide existing = userDataHideMapper.selectByUserAndRef(userId, "account", accountId);
            if (existing == null) {
                UserDataHide hide = new UserDataHide();
                hide.setUserId(userId);
                hide.setDataType("account");
                hide.setRefId(accountId);
                userDataHideMapper.insert(hide);
            }
        } else {
            accountMapper.softDeleteById(accountId);
        }
    }

    /**
     * 获取账户交易流水（含分类和关联账户信息）
     */
    public PageResult<TransactionVO> getAccountTransactions(Long accountId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        List<Transaction> transactions = transactionMapper.selectByAccountId(accountId, offset, pageSize);
        Long total = transactionMapper.countByAccountId(accountId);

        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = new TransactionVO();
            BeanUtils.copyProperties(transaction, vo);
            voList.add(vo);
        }

        if (!voList.isEmpty()) {
            List<Long> txIds = voList.stream().map(TransactionVO::getId).collect(Collectors.toList());
            List<Entry> allEntries = entryMapper.selectByTransactionIds(txIds);

            Map<Long, List<Entry>> entryMap = allEntries.stream()
                    .collect(Collectors.groupingBy(Entry::getTransactionId));

            for (TransactionVO vo : voList) {
                List<Entry> entries = entryMap.getOrDefault(vo.getId(), new ArrayList<>());
                fillCategories(vo, entries);
                fillRelatedAccount(vo, entries, accountId);
            }
        }

        boolean hasMore = (page * pageSize) < total;
        PageResult<TransactionVO> result = PageResult.of(voList, page, pageSize, hasMore);
        result.setTotal(total);
        return result;
    }

    private void fillCategories(TransactionVO vo, List<Entry> entries) {
        for (Entry entry : entries) {
            if ("category".equals(entry.getAccountType())) {
                Category category = categoryMapper.selectById(entry.getAccountId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                    vo.setCategoryIcon(category.getIcon());
                    if (category.getParentId() != null) {
                        Category parent = categoryMapper.selectById(category.getParentId());
                        if (parent != null) {
                            vo.setParentCategoryName(parent.getName());
                            vo.setParentCategoryIcon(parent.getIcon());
                        }
                    }
                }
                return;
            }
        }
    }

    private void fillRelatedAccount(TransactionVO vo, List<Entry> entries, Long currentAccountId) {
        String type = vo.getTransactionType();
        if (!"transfer".equals(type) && !"repayment".equals(type)) {
            return;
        }

        for (Entry entry : entries) {
            if ("account".equals(entry.getAccountType()) && !entry.getAccountId().equals(currentAccountId)) {
                Account related = accountMapper.selectById(entry.getAccountId());
                if (related != null) {
                    vo.setRelatedAccountName(related.getName());
                    vo.setRelatedAccountIcon(related.getIcon());
                }
                return;
            }
        }
    }

    /**
     * 调整账户余额
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustBalance(Long accountId, BigDecimal newBalance, String reason) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        // 更新初始余额
        Account updateAccount = new Account();
        updateAccount.setId(accountId);
        updateAccount.setInitialBalance(newBalance);
        updateAccount.setUpdatedAt(LocalDateTime.now());
        accountMapper.updateById(updateAccount);
    }

    /**
     * 统计账户在指定时间范围内的收支情况
     */
    public AccountStatisticsVO getAccountStatistics(Long accountId, String startDate, String endDate) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        Map<String, Object> stats = transactionMapper.calculateAccountStatistics(accountId, startDate, endDate);
        BigDecimal income = toBigDecimal(stats.get("income"));
        BigDecimal expense = toBigDecimal(stats.get("expense"));
        Long transactionCount = toLong(stats.get("transactionCount"));

        return new AccountStatisticsVO(income, expense, transactionCount);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        return new BigDecimal(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    /**
     * 转换为Map
     */
    private Map<String, Object> convertToMap(Account account) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", account.getId());
        map.put("name", account.getName());
        map.put("type", account.getType());
        map.put("icon", account.getIcon());
        map.put("color", account.getColor());
        map.put("initialBalance", account.getInitialBalance());
        map.put("currentBalance", account.getCurrentBalance());
        map.put("creditLimit", account.getCreditLimit());
        map.put("includeInNetWorth", account.getIncludeInNetWorth());
        map.put("allowTransfer", account.getAllowTransfer());
        map.put("enableAlert", account.getEnableAlert());
        map.put("alertThreshold", account.getAlertThreshold());
        map.put("sortOrder", account.getSortOrder());
        map.put("desc", account.getDesc());
        map.put("createdAt", account.getCreatedAt());
        map.put("updatedAt", account.getUpdatedAt());
        return map;
    }
}
