package com.duoduocode.service.billimport;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.billimport.mapper.ImportBatchMapper;
import com.duoduocode.service.recalculation.service.RecalculationTaskService;
import com.duoduocode.service.transaction.dto.TransactionDTO;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import com.duoduocode.service.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 账单导入服务：parse 无状态预览 / confirm 整批单事务落库（复用既有记账链路）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    /** 源文件大小上限（前端同限） */
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    /** 未命中映射时的兜底分类名 */
    private static final String FALLBACK_EXPENSE_CATEGORY = "其他支出";
    private static final String FALLBACK_INCOME_CATEGORY = "其他收入";

    /** R003 异步受理阈值：可导入行 ≥50 转后台任务 */
    private static final int ASYNC_IMPORT_THRESHOLD = 50;

    private final BillCsvParser parser = new BillCsvParser();

    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;
    private final TransactionService transactionService;
    private final BillAccountMatcher accountMatcher;
    private final ImportBatchMapper importBatchMapper;

    /** 异步任务服务走字段注入（@Lazy 打破构造循环）；同步路径经 self 代理保事务 */
    @Autowired
    @Lazy
    private RecalculationTaskService recalculationTaskService;

    @Autowired
    @Lazy
    private ImportService self;

    // ===== 静态工具（单测覆盖） =====

    /**
     * 账单行 → 交易 DTO（direction 收→income / 支→expense；含导入溯源字段）
     */
    public static TransactionDTO buildTransactionDTO(BillCsvParser.Row row,
                                                     Long categoryId, Long accountId, Long importBatchId) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionType(row.getDirection());
        dto.setDate(LocalDate.parse(row.getDate()));
        dto.setTime(LocalTime.parse(row.getTime()));
        dto.setAmount(row.getAmount());
        dto.setDescription(row.getDescription());
        dto.setCategoryId(categoryId);
        dto.setAccountId(accountId);
        dto.setMode("simple");
        dto.setExternalOrderNo(row.getOrderNo());
        dto.setImportBatchId(importBatchId);
        return dto;
    }

    /**
     * 单号判重：同用户同单号即视为已导入
     */
    public static boolean isDuplicate(TransactionMapper mapper, Long userId, String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return false;
        }
        Long count = mapper.countByUserAndOrderNo(userId, orderNo);
        return count != null && count > 0;
    }

    // ===== parse（无状态预览） =====

    public ImportParseVO parse(Long userId, ImportParseDTO dto) {
        BillCsvParser.ParseResult result = doParse(dto);
        return buildPreview(userId, result);
    }

    // ===== confirm（整批单事务） =====

    /**
     * R003 导入受理入口：可导入行 ≥50 转异步任务（返回 taskId），
     * 否则同步执行（经 self 代理走既有单事务路径）。
     */
    public ImportConfirmVO confirmOrDispatch(Long userId, ImportConfirmDTO dto) {
        BillCsvParser.ParseResult result = doParse(dto);
        int importable = countImportable(result);
        if (importable >= ASYNC_IMPORT_THRESHOLD) {
            Long taskId = recalculationTaskService.createTask(userId, "import", importable);
            recalculationTaskService.executeImport(taskId, userId, dto,
                    () -> self.confirm(userId, dto));
            ImportConfirmVO vo = new ImportConfirmVO();
            vo.setTaskId(taskId);
            return vo;
        }
        return self.confirm(userId, dto);
    }

    private int countImportable(BillCsvParser.ParseResult result) {
        int count = 0;
        for (BillCsvParser.Row row : result.getRows()) {
            if ("importable".equals(row.getStatus())) {
                count++;
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportConfirmVO confirm(Long userId, ImportConfirmDTO dto) {
        boolean allowDuplicates = dto != null && Boolean.TRUE.equals(dto.getAllowDuplicates());
        BillCsvParser.ParseResult result = doParse(dto);

        ImportBatch batch = new ImportBatch();
        batch.setUserId(userId);
        batch.setSource(result.getSource());
        batch.setFileName(dto.getFileName());
        batch.setTotalRows(result.getTotalRows());
        batch.setImportedCount(0);
        batch.setSkippedCount(0);
        batch.setIgnoredCount(result.getIgnoredCount());
        batch.setCreatedAccounts(null);
        batch.setStatus("confirmed");
        importBatchMapper.insert(batch);
        Long batchId = batch.getId();

        List<Account> createdAccounts = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        for (BillCsvParser.Row row : result.getRows()) {
            if (!"importable".equals(row.getStatus())) {
                continue;
            }
            if (!allowDuplicates && isDuplicate(transactionMapper, userId, row.getOrderNo())) {
                skipped++;
                continue;
            }
            Long accountId = accountMatcher.match(userId, row.getPayMethod(), createdAccounts).getAccountId();
            Long categoryId = resolveCategoryId(userId, row);
            TransactionDTO txDto = buildTransactionDTO(row, categoryId, accountId, batchId);
            transactionService.createTransaction(userId, txDto);
            imported++;
        }

        Set<String> names = new LinkedHashSet<>();
        for (Account account : createdAccounts) {
            names.add(account.getName());
        }
        String createdAccountsText = names.isEmpty() ? null : String.join(",", names);
        importBatchMapper.updateCounts(batchId, imported, skipped, result.getIgnoredCount(), createdAccountsText);

        ImportConfirmVO vo = new ImportConfirmVO();
        vo.setBatchId(batchId);
        vo.setImportedCount(imported);
        vo.setSkippedCount(skipped);
        vo.setIgnoredCount(result.getIgnoredCount());
        vo.setCreatedAccounts(new ArrayList<>(names));
        return vo;
    }

    // ===== 内部流程 =====

    private BillCsvParser.ParseResult doParse(ImportParseDTO dto) {
        if (dto == null || dto.getContentBase64() == null || dto.getContentBase64().trim().isEmpty()) {
            throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件内容不能为空");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dto.getContentBase64().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件内容不是合法的 base64");
        }
        if (bytes.length == 0) {
            throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件为空");
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件超过 5MB 上限");
        }
        return parser.parse(dto.getFileName(), bytes);
    }

    /**
     * 预览组装：判重标记 + 新账户清单 + 账户名回填
     */
    private ImportParseVO buildPreview(Long userId, BillCsvParser.ParseResult result) {
        // 判重预查（分批 IN）
        Set<String> orderNos = new HashSet<>();
        for (BillCsvParser.Row row : result.getRows()) {
            if ("importable".equals(row.getStatus()) && row.getOrderNo() != null && !row.getOrderNo().isEmpty()) {
                orderNos.add(row.getOrderNo());
            }
        }
        Set<String> existingOrderNos = queryExistingOrderNos(userId, orderNos);

        Set<String> existingAccountNames = new HashSet<>();
        for (Account account : accountMapper.selectByUserId(userId)) {
            existingAccountNames.add(account.getName());
        }

        ImportParseVO vo = new ImportParseVO();
        vo.setSource(result.getSource());
        vo.setTotalRows(result.getTotalRows());
        vo.setIgnoredCount(result.getIgnoredCount());

        int importable = 0;
        int duplicate = 0;
        List<ImportParseVO.RowVO> rowVOs = new ArrayList<>();
        Set<String> newAccounts = new LinkedHashSet<>();

        for (BillCsvParser.Row row : result.getRows()) {
            ImportParseVO.RowVO rowVO = new ImportParseVO.RowVO();
            rowVO.setLineNo(row.getLineNo());
            rowVO.setDate(row.getDate());
            rowVO.setTime(row.getTime());
            rowVO.setDescription(row.getDescription());
            rowVO.setCounterparty(row.getCounterparty());
            rowVO.setAmount(row.getAmount());
            rowVO.setDirection(row.getDirection());
            rowVO.setCategoryName(row.getCategoryName());
            rowVO.setPayMethod(row.getPayMethod());
            rowVO.setOrderNo(row.getOrderNo());
            rowVO.setStatus(row.getStatus());
            rowVO.setReason(row.getReason());

            String accountName = row.getPayMethod() == null || row.getPayMethod().trim().isEmpty()
                    ? BillAccountMatcher.CASH_ACCOUNT_NAME : row.getPayMethod().trim();
            rowVO.setAccountName(accountName);

            if ("importable".equals(row.getStatus())) {
                if (row.getOrderNo() != null && existingOrderNos.contains(row.getOrderNo())) {
                    rowVO.setStatus("duplicate");
                    rowVO.setReason("已有相同交易单号");
                    duplicate++;
                } else {
                    importable++;
                    if (!existingAccountNames.contains(accountName)) {
                        newAccounts.add(accountName);
                    }
                }
            }
            rowVOs.add(rowVO);
        }

        vo.setRows(rowVOs);
        vo.setImportableCount(importable);
        vo.setDuplicateCount(duplicate);
        vo.setNewAccounts(new ArrayList<>(newAccounts));
        return vo;
    }

    private Set<String> queryExistingOrderNos(Long userId, Set<String> orderNos) {
        Set<String> existing = new HashSet<>();
        if (orderNos.isEmpty()) {
            return existing;
        }
        List<String> all = new ArrayList<>(orderNos);
        int chunk = 500;
        for (int i = 0; i < all.size(); i += chunk) {
            List<String> part = all.subList(i, Math.min(i + chunk, all.size()));
            existing.addAll(transactionMapper.selectExistingOrderNos(userId, part));
        }
        return existing;
    }

    /**
     * 分类解析：映射名 → 用户分类（按方向类型）；兜底「其他支出/其他收入」→ 创建「未分类」
     */
    private Long resolveCategoryId(Long userId, BillCsvParser.Row row) {
        String type = "income".equals(row.getDirection()) ? "income" : "expense";
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, type);
        Map<String, Category> byName = new LinkedHashMap<>();
        for (Category c : categories) {
            byName.putIfAbsent(c.getName(), c);
        }

        if (row.getCategoryName() != null) {
            Category hit = byName.get(row.getCategoryName());
            if (hit != null) {
                return hit.getId();
            }
        }
        String fallbackName = "income".equals(type) ? FALLBACK_INCOME_CATEGORY : FALLBACK_EXPENSE_CATEGORY;
        Category fallback = byName.get(fallbackName);
        if (fallback != null) {
            return fallback.getId();
        }
        Category unclassified = byName.get("未分类");
        if (unclassified != null) {
            return unclassified.getId();
        }
        return createUnclassifiedCategory(userId, type);
    }

    private Long createUnclassifiedCategory(Long userId, String type) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName("未分类");
        category.setType(type);
        category.setSortOrder(99);
        category.setIsDeleted(false);
        category.setCreatedAt(java.time.LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }
}
