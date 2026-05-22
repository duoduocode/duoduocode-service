package com.duoduocode.service.account.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.transaction.dto.TransactionVO;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import com.duoduocode.service.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户服务类
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

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

            if ("asset".equals(type)) {
                grouped.get("asset").add(accountMap);
                totalAssets = totalAssets.add(account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO);
            } else if ("liability".equals(type)) {
                grouped.get("liability").add(accountMap);
                totalLiabilities = totalLiabilities.add(account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO);
            } else if ("investment".equals(type)) {
                grouped.get("investment").add(accountMap);
                totalInvestments = totalInvestments.add(account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO);
            }
        }

        // 计算净资产
        BigDecimal netWorth = totalAssets.add(totalInvestments).add(totalLiabilities);

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

        // 检查名称是否重复
        int count = accountMapper.countByUserIdAndName(userId, name, null);
        if (count > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "账户名称已存在");
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
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        accountMapper.insert(account);
        return account.getId();
    }

    /**
     * 更新账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(Long accountId, Map<String, Object> dto) {
        Account existing = accountMapper.selectById(accountId);
        if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        Account account = new Account();
        account.setId(accountId);
        account.setUpdatedAt(LocalDateTime.now());

        if (dto.containsKey("name")) {
            String name = (String) dto.get("name");
            if (name != null && !name.trim().isEmpty()) {
                // 检查名称是否重复
                int count = accountMapper.countByUserIdAndName(existing.getUserId(), name, accountId);
                if (count > 0) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "账户名称已存在");
                }
                account.setName(name.trim());
            }
        }

        if (dto.containsKey("icon")) {
            account.setIcon((String) dto.get("icon"));
        }
        if (dto.containsKey("color")) {
            account.setColor((String) dto.get("color"));
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
            account.setAlertThreshold(new BigDecimal(dto.get("alertThreshold").toString()));
        }
        if (dto.containsKey("sortOrder")) {
            account.setSortOrder((Integer) dto.get("sortOrder"));
        }

        accountMapper.updateById(account);
    }

    /**
     * 删除账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        accountMapper.softDeleteById(accountId);
    }

    /**
     * 获取账户交易流水
     */
    public PageResult<TransactionVO> getAccountTransactions(Long accountId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        // 查询交易列表
        List<Transaction> transactions = transactionMapper.selectByAccountId(accountId, offset, pageSize);

        // 查询总数
        Long total = transactionMapper.countByAccountId(accountId);

        // 转换为VO
        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = new TransactionVO();
            BeanUtils.copyProperties(transaction, vo);
            voList.add(vo);
        }

        boolean hasMore = (page * pageSize) < total;
        PageResult<TransactionVO> result = PageResult.of(voList, page, pageSize, hasMore);
        result.setTotal(total);
        return result;
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
        map.put("creditLimit", account.getCreditLimit());
        map.put("includeInNetWorth", account.getIncludeInNetWorth());
        map.put("allowTransfer", account.getAllowTransfer());
        map.put("enableAlert", account.getEnableAlert());
        map.put("alertThreshold", account.getAlertThreshold());
        map.put("sortOrder", account.getSortOrder());
        map.put("createdAt", account.getCreatedAt());
        map.put("updatedAt", account.getUpdatedAt());
        return map;
    }
}
