package com.duoduocode.service.transaction.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.common.dto.PageResult;
import com.duoduocode.service.transaction.dto.*;
import com.duoduocode.service.transaction.entity.Entry;
import com.duoduocode.service.transaction.entity.Transaction;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易服务类
 * 复式记账核心逻辑实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionMapper transactionMapper;
    private final EntryMapper entryMapper;
    private final AccountMapper accountMapper;

    // 交易类型常量
    private static final String TYPE_EXPENSE = "expense";
    private static final String TYPE_INCOME = "income";
    private static final String TYPE_TRANSFER = "transfer";
    private static final String TYPE_REPAYMENT = "repayment";

    // 记账模式常量
    private static final String MODE_SIMPLE = "simple";
    private static final String MODE_FULL = "full";

    // 退款状态常量
    private static final String REFUND_NONE = "none";
    private static final String REFUND_PARTIAL = "partial";
    private static final String REFUND_FULL = "full";

    /**
     * 获取交易列表（分页）
     *
     * @param userId 用户ID
     * @param query  查询参数
     * @return 分页交易列表
     */
    public PageResult<TransactionVO> getTransactionList(Long userId, TransactionQuery query) {
        // 查询交易列表
        List<Transaction> transactions = transactionMapper.selectByUserId(
                userId,
                query.getOffset(),
                query.getPageSize(),
                query.getStartDate(),
                query.getEndDate()
        );

        // 查询总数
        Long total = transactionMapper.countByUserId(userId, query.getStartDate(), query.getEndDate());

        // 转换为VO
        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = convertToVO(transaction);
            voList.add(vo);
        }

        // 计算是否有更多
        boolean hasMore = (query.getPage() * query.getPageSize()) < total;

        PageResult<TransactionVO> result = PageResult.of(voList, query.getPage(), query.getPageSize(), hasMore);
        result.setTotal(total);
        return result;
    }

    /**
     * 获取最近交易
     *
     * @param userId 用户ID
     * @param limit  限制条数
     * @return 最近交易列表
     */
    public List<TransactionVO> getRecentTransactions(Long userId, int limit) {
        List<Transaction> transactions = transactionMapper.selectRecentByUserId(userId, limit);
        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = convertToVO(transaction);
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 获取交易详情
     *
     * @param id 交易ID
     * @return 交易详情
     */
    public TransactionVO getTransactionDetail(Long id) {
        Transaction transaction = transactionMapper.selectById(id);
        if (transaction == null || Integer.valueOf(1).equals(transaction.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "交易不存在");
        }
        return convertToVO(transaction);
    }

    /**
     * 创建交易
     *
     * @param userId 用户ID
     * @param dto    交易数据
     * @return 创建的交易ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTransaction(Long userId, TransactionDTO dto) {
        // 验证必填参数
        validateTransactionDTO(dto);

        // 创建交易实体
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        transaction.setTime(dto.getTime() != null ? dto.getTime() : LocalTime.now());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setMode(dto.getMode() != null ? dto.getMode() : MODE_SIMPLE);
        transaction.setTransactionType(dto.getTransactionType() != null ? dto.getTransactionType() : TYPE_EXPENSE);
        transaction.setRefundStatus(REFUND_NONE);
        transaction.setRefundedAmount(BigDecimal.ZERO);
        transaction.setIsDeleted(0);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // 插入交易
        transactionMapper.insert(transaction);
        Long transactionId = transaction.getId();

        // 根据模式生成分录
        if (MODE_FULL.equals(transaction.getMode())) {
            // 完整模式：用户手动录入分录
            createFullModeEntries(transactionId, dto.getEntries());
        } else {
            // 简化模式：根据交易类型自动生成分录
            createSimpleModeEntries(transactionId, dto);
        }

        return transactionId;
    }

    /**
     * 更新交易
     *
     * @param id  交易ID
     * @param dto 交易数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTransaction(Long id, TransactionDTO dto) {
        // 检查交易是否存在
        Transaction existing = transactionMapper.selectById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "交易不存在");
        }

        // 检查是否已退款
        if (!REFUND_NONE.equals(existing.getRefundStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已退款的交易不能修改");
        }

        // 验证参数
        validateTransactionDTO(dto);

        // 更新交易
        Transaction transaction = new Transaction();
        transaction.setId(id);
        if (dto.getDate() != null) transaction.setDate(dto.getDate());
        if (dto.getTime() != null) transaction.setTime(dto.getTime());
        if (dto.getAmount() != null) transaction.setAmount(dto.getAmount());
        if (dto.getDescription() != null) transaction.setDescription(dto.getDescription());
        if (dto.getMode() != null) transaction.setMode(dto.getMode());

        transactionMapper.updateById(transaction);

        // 删除原有分录
        entryMapper.deleteByTransactionId(id);

        // 重新生成分录
        String mode = dto.getMode() != null ? dto.getMode() : existing.getMode();
        if (MODE_FULL.equals(mode)) {
            createFullModeEntries(id, dto.getEntries());
        } else {
            createSimpleModeEntries(id, dto);
        }
    }

    /**
     * 删除交易（软删除）
     *
     * @param id 交易ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionMapper.selectById(id);
        if (transaction == null || Integer.valueOf(1).equals(transaction.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "交易不存在");
        }

        // 软删除交易
        transactionMapper.softDeleteById(id);
    }

    /**
     * 退款处理（完善版）
     *
     * @param id  交易ID
     * @param dto 退款数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundTransaction(Long id, RefundDTO dto) {
        // 1. 检查交易是否存在
        Transaction transaction = transactionMapper.selectById(id);
        if (transaction == null || Integer.valueOf(1).equals(transaction.getIsDeleted())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "交易不存在");
        }

        // 2. 校验原交易类型为支出（通过分录判断）
        List<Entry> originalEntries = entryMapper.selectByTransactionId(id);
        boolean isExpense = false;
        for (Entry entry : originalEntries) {
            if ("category".equals(entry.getAccountType()) && entry.getDebit() != null && entry.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                isExpense = true;
                break;
            }
        }
        if (!isExpense) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只有支出交易可以退款");
        }

        // 3. 验证退款金额
        BigDecimal refundAmount = dto.getAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款金额必须大于0");
        }

        // 4. 校验退款金额 <= 原交易金额
        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "退款金额不能超过原交易金额");
        }

        BigDecimal alreadyRefunded = transaction.getRefundedAmount() != null
                ? transaction.getRefundedAmount() : BigDecimal.ZERO;
        BigDecimal maxRefundable = transaction.getAmount().subtract(alreadyRefunded);

        // 5. 校验未全额退款
        if (alreadyRefunded.compareTo(transaction.getAmount()) >= 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该交易已全额退款");
        }

        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "退款金额超过可退金额，最多可退: " + maxRefundable);
        }

        // 6. 计算新的退款状态
        BigDecimal newRefundedAmount = alreadyRefunded.add(refundAmount);
        String newRefundStatus;
        if (newRefundedAmount.compareTo(transaction.getAmount()) >= 0) {
            newRefundStatus = REFUND_FULL;
        } else {
            newRefundStatus = REFUND_PARTIAL;
        }

        // 7. 更新退款状态
        transactionMapper.updateRefundStatus(id, newRefundStatus, newRefundedAmount.toString());

        // 8. 创建退款分录（反向分录）
        createRefundEntries(id, refundAmount, dto.getDescription(), dto.getAccountId());

        // 9. 更新账户余额（通过创建反向分录实现）
        // 分录已创建，账户余额会在查询时自动计算

        // 10. 还原预算占用（通过分录实现）
        // 分录已创建，预算占用会在统计时自动计算
    }

    /**
     * 重复交易检测
     *
     * @param userId 用户ID
     * @param dto    检测参数
     * @return 检测结果
     */
    public Map<String, Object> checkDuplicate(Long userId, DuplicateCheckDTO dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", false);
        result.put("existingTransaction", null);

        if (dto.getAmount() == null || dto.getDate() == null) {
            return result;
        }

        // 查找可能的重复交易
        Transaction existing = transactionMapper.checkDuplicate(
                userId,
                dto.getAmount().toString(),
                dto.getDate(),
                dto.getDescription()
        );

        if (existing != null) {
            result.put("isDuplicate", true);
            result.put("existingTransaction", convertToVO(existing));
        }

        return result;
    }

    /**
     * 搜索筛选交易（旧版，兼容）
     *
     * @param userId 用户ID
     * @param query  搜索参数
     * @return 分页交易列表
     */
    public PageResult<TransactionVO> searchTransactions(Long userId, SearchQuery query) {
        // 搜索交易
        List<Transaction> transactions = transactionMapper.search(
                userId,
                query.getKeyword(),
                query.getAccountId(),
                query.getMinAmount(),
                query.getMaxAmount(),
                query.getStartDate(),
                query.getEndDate(),
                query.getOffset(),
                query.getPageSize()
        );

        // 搜索总数
        Long total = transactionMapper.countSearch(
                userId,
                query.getKeyword(),
                query.getAccountId(),
                query.getMinAmount(),
                query.getMaxAmount(),
                query.getStartDate(),
                query.getEndDate()
        );

        // 转换为VO
        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = convertToVO(transaction);
            voList.add(vo);
        }

        // 计算是否有更多
        boolean hasMore = (query.getPage() * query.getPageSize()) < total;

        PageResult<TransactionVO> result = PageResult.of(voList, query.getPage(), query.getPageSize(), hasMore);
        result.setTotal(total);
        return result;
    }

    /**
     * 高级搜索交易（增强版）
     *
     * @param userId 用户ID
     * @param query  搜索参数
     * @return 分页交易列表
     */
    public PageResult<TransactionVO> searchTransactions(Long userId, TransactionSearchQuery query) {
        // 搜索交易
        List<Transaction> transactions = transactionMapper.searchTransactions(userId, query);

        // 搜索总数
        Long total = transactionMapper.countSearchTransactions(userId, query);

        // 转换为VO
        List<TransactionVO> voList = new ArrayList<>();
        for (Transaction transaction : transactions) {
            TransactionVO vo = convertToVO(transaction);
            voList.add(vo);
        }

        // 计算是否有更多
        boolean hasMore = (query.getPage() * query.getPageSize()) < total;

        PageResult<TransactionVO> result = PageResult.of(voList, query.getPage(), query.getPageSize(), hasMore);
        result.setTotal(total);
        return result;
    }

    // ===== 复式记账核心逻辑 =====

    /**
     * 简化模式：根据交易类型自动生成分录
     */
    private void createSimpleModeEntries(Long transactionId, TransactionDTO dto) {
        String transactionType = dto.getTransactionType();
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交易类型不能为空");
        }

        List<Entry> entries = new ArrayList<>();
        BigDecimal amount = dto.getAmount();
        LocalDateTime now = LocalDateTime.now();

        switch (transactionType) {
            case TYPE_EXPENSE:
                // 支出：借-分类，贷-账户
                validateExpenseParams(dto);
                entries.add(createEntry(transactionId, dto.getCategoryId(), amount, null, "category", now));
                entries.add(createEntry(transactionId, dto.getAccountId(), null, amount, "account", now));
                break;

            case TYPE_INCOME:
                // 收入：借-账户，贷-分类
                validateIncomeParams(dto);
                entries.add(createEntry(transactionId, dto.getAccountId(), amount, null, "account", now));
                entries.add(createEntry(transactionId, dto.getCategoryId(), null, amount, "category", now));
                break;

            case TYPE_TRANSFER:
                // 转账：借-目标账户，贷-源账户
                validateTransferParams(dto);
                entries.add(createEntry(transactionId, dto.getTargetAccountId(), amount, null, "account", now));
                entries.add(createEntry(transactionId, dto.getAccountId(), null, amount, "account", now));
                break;

            case TYPE_REPAYMENT:
                // 还款：借-负债账户，贷-资产账户
                validateRepaymentParams(dto);
                entries.add(createEntry(transactionId, dto.getCategoryId(), amount, null, "account", now));
                entries.add(createEntry(transactionId, dto.getAccountId(), null, amount, "account", now));
                break;

            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "无效的交易类型: " + transactionType);
        }

        // 批量插入分录
        if (!entries.isEmpty()) {
            entryMapper.batchInsert(entries);
        }
    }

    /**
     * 完整模式：用户手动录入分录，校验借贷平衡
     */
    private void createFullModeEntries(Long transactionId, List<EntryDTO> entryDTOs) {
        if (entryDTOs == null || entryDTOs.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分录列表不能为空");
        }

        // 计算借贷总额
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<Entry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (EntryDTO dto : entryDTOs) {
            if (dto.getAccountId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "分录账户ID不能为空");
            }

            BigDecimal debit = dto.getDebit() != null ? dto.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = dto.getCredit() != null ? dto.getCredit() : BigDecimal.ZERO;

            // 每条分录的借方或贷方必须有一个为0
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "分录不能同时有借方和贷方金额");
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);

            entries.add(createEntry(transactionId, dto.getAccountId(),
                    debit.compareTo(BigDecimal.ZERO) > 0 ? debit : null,
                    credit.compareTo(BigDecimal.ZERO) > 0 ? credit : null,
                    dto.getAccountType() != null ? dto.getAccountType() : "account", now));
        }

        // 校验借贷平衡
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "借贷不平衡，借方总额: " + totalDebit + "，贷方总额: " + totalCredit);
        }

        // 批量插入分录
        entryMapper.batchInsert(entries);
    }

    /**
     * 创建退款分录（反向分录）
     */
    private void createRefundEntries(Long originalTransactionId, BigDecimal refundAmount, String description, Long refundAccountId) {
        // 查询原交易的分录
        List<Entry> originalEntries = entryMapper.selectByTransactionId(originalTransactionId);
        if (originalEntries.isEmpty()) {
            return;
        }

        List<Entry> refundEntries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 计算原交易的借方总额（用于计算比例）
        BigDecimal originalTotalDebit = entryMapper.sumDebitByTransactionId(originalTransactionId);
        if (originalTotalDebit == null || originalTotalDebit.compareTo(BigDecimal.ZERO) == 0) {
            originalTotalDebit = refundAmount;
        }

        // 计算退款比例
        BigDecimal ratio = refundAmount.divide(originalTotalDebit, 4, BigDecimal.ROUND_HALF_UP);

        for (Entry original : originalEntries) {
            BigDecimal originalDebit = original.getDebit() != null ? original.getDebit() : BigDecimal.ZERO;
            BigDecimal originalCredit = original.getCredit() != null ? original.getCredit() : BigDecimal.ZERO;

            // 反向分录：借变贷，贷变借
            Entry refundEntry = new Entry();
            refundEntry.setTransactionId(originalTransactionId);
            refundEntry.setAccountId(original.getAccountId());
            refundEntry.setAccountType(original.getAccountType());
            refundEntry.setIsDeleted(0);
            refundEntry.setCreatedAt(now);

            if (originalDebit.compareTo(BigDecimal.ZERO) > 0) {
                // 原借方变为贷方
                refundEntry.setDebit(null);
                refundEntry.setCredit(originalDebit.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP));
            } else if (originalCredit.compareTo(BigDecimal.ZERO) > 0) {
                // 原贷方变为借方
                refundEntry.setDebit(originalCredit.multiply(ratio).setScale(2, BigDecimal.ROUND_HALF_UP));
                refundEntry.setCredit(null);
            }

            refundEntries.add(refundEntry);
        }

        // 如果指定了退款入账账户，添加特殊的退款分录
        if (refundAccountId != null) {
            // 添加退款账户的借方分录（资产增加）
            Entry refundAccountEntry = new Entry();
            refundAccountEntry.setTransactionId(originalTransactionId);
            refundAccountEntry.setAccountId(refundAccountId);
            refundAccountEntry.setAccountType("account");
            refundAccountEntry.setIsDeleted(0);
            refundAccountEntry.setDebit(refundAmount);
            refundAccountEntry.setCredit(null);
            refundAccountEntry.setCreatedAt(now);
            refundEntries.add(refundAccountEntry);
        }

        // 批量插入退款分录
        if (!refundEntries.isEmpty()) {
            entryMapper.batchInsert(refundEntries);
        }
    }

    /**
     * 创建分录实体
     */
    private Entry createEntry(Long transactionId, Long accountId, BigDecimal debit,
                              BigDecimal credit, String accountType, LocalDateTime createdAt) {
        Entry entry = new Entry();
        entry.setTransactionId(transactionId);
        entry.setAccountId(accountId);
        entry.setDebit(debit);
        entry.setCredit(credit);
        entry.setAccountType(accountType);
        entry.setIsDeleted(0);
        entry.setCreatedAt(createdAt);
        return entry;
    }

    // ===== 参数验证方法 =====

    private void validateTransactionDTO(TransactionDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交易金额必须大于0");
        }
    }

    private void validateExpenseParams(TransactionDTO dto) {
        if (dto.getAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支出交易必须指定账户");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支出交易必须指定分类");
        }
    }

    private void validateIncomeParams(TransactionDTO dto) {
        if (dto.getAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收入交易必须指定账户");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收入交易必须指定分类");
        }
    }

    private void validateTransferParams(TransactionDTO dto) {
        if (dto.getAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转账交易必须指定源账户");
        }
        if (dto.getTargetAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转账交易必须指定目标账户");
        }
        if (dto.getAccountId().equals(dto.getTargetAccountId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "源账户和目标账户不能相同");
        }
    }

    private void validateRepaymentParams(TransactionDTO dto) {
        if (dto.getAccountId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "还款交易必须指定资产账户");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "还款交易必须指定负债账户");
        }
    }

    // ===== 转换方法 =====

    /**
     * 将实体转换为VO
     */
    private TransactionVO convertToVO(Transaction transaction) {
        TransactionVO vo = new TransactionVO();
        BeanUtils.copyProperties(transaction, vo);

        // 查询分录
        List<Entry> entries = entryMapper.selectByTransactionId(transaction.getId());
        List<EntryVO> entryVOs = new ArrayList<>();

        // 用于判断交易类型
        Long accountId = null;
        Long categoryId = null;
        String transactionType = null;

        for (Entry entry : entries) {
            EntryVO entryVO = new EntryVO();
            BeanUtils.copyProperties(entry, entryVO);

            // 查询账户/分类名称
            Account account = accountMapper.selectById(entry.getAccountId());
            if (account != null) {
                entryVO.setAccountName(account.getName());
                entryVO.setAccountIcon(account.getIcon());
            }

            entryVOs.add(entryVO);

            // 判断交易类型
            if ("account".equals(entry.getAccountType())) {
                if (entry.getDebit() != null && entry.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                    accountId = entry.getAccountId();
                    if (transactionType == null) {
                        transactionType = TYPE_INCOME; // 借-账户，可能是收入或转账目标
                    }
                }
                if (entry.getCredit() != null && entry.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                    if (accountId == null) {
                        accountId = entry.getAccountId();
                    }
                    if (transactionType == null || transactionType.equals(TYPE_INCOME)) {
                        transactionType = TYPE_EXPENSE; // 贷-账户，可能是支出或转账源
                    }
                }
            } else if ("category".equals(entry.getAccountType())) {
                if (entry.getDebit() != null && entry.getDebit().compareTo(BigDecimal.ZERO) > 0) {
                    categoryId = entry.getAccountId();
                    transactionType = TYPE_EXPENSE; // 借-分类，支出
                }
                if (entry.getCredit() != null && entry.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                    categoryId = entry.getAccountId();
                    transactionType = TYPE_INCOME; // 贷-分类，收入
                }
            }
        }

        vo.setEntries(entryVOs);
        // 使用数据库中的原始 transactionType，而不是通过分录推断的值
        // vo.setTransactionType(transactionType);

        // 设置账户和分类名称
        if (accountId != null) {
            Account account = accountMapper.selectById(accountId);
            if (account != null) {
                vo.setAccountName(account.getName());
            }
        }

        return vo;
    }
}
