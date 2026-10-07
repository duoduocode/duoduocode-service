package com.duoduocode.service.export;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.budget.entity.DailyBudget;
import com.duoduocode.service.budget.entity.SpecialBudget;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
import com.duoduocode.service.budget.mapper.SpecialBudgetMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.transaction.entity.Transaction;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出服务：CSV（UTF-8 BOM，含借/贷双列）与 JSON（全量备份，含软删除标记数据）。
 * 构建方法为纯函数（单测直调）；数据装载依赖 Mapper，单测无数据源时输出空集合骨架。
 */
@Slf4j
@Service
public class ExportService {

    private static final String CSV_HEADER = "日期,时间,类型,分类,金额,账户,备注,借方,贷方,来源单号";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired(required = false)
    private TransactionMapper transactionMapper;

    @Autowired(required = false)
    private EntryMapper entryMapper;

    @Autowired(required = false)
    private AccountMapper accountMapper;

    @Autowired(required = false)
    private CategoryMapper categoryMapper;

    @Autowired(required = false)
    private DailyBudgetMapper dailyBudgetMapper;

    @Autowired(required = false)
    private SpecialBudgetMapper specialBudgetMapper;

    @Autowired(required = false)
    private ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /* ────────── 生产入口 ────────── */

    /**
     * CSV 导出：全部未删除交易
     */
    public byte[] exportCsv(Long userId) {
        List<Transaction> txs = loadTransactions(userId, false);
        Map<Long, List<Map<String, Object>>> entryRows = loadEntryRows(txs);
        return buildCsvBytes(userId, csvTransactionRows(txs, entryRows), entryPairRows(txs, entryRows));
    }

    /**
     * JSON 导出：全量备份（账户/分类/交易含分录/预算，交易含软删除）
     */
    public byte[] exportJson(Long userId) {
        return buildJsonText(userId).getBytes(StandardCharsets.UTF_8);
    }

    /* ────────── CSV 构建（纯函数） ────────── */

    /**
     * 生成 CSV 文本（无 BOM，\n 分行，无行尾换行）
     */
    public String buildCsvText(Long userId, List<Map<String, Object>> transactions,
                               List<Map<String, Object>> entries) {
        Map<Long, List<Map<String, Object>>> entriesByTx = new LinkedHashMap<>();
        if (entries != null) {
            for (Map<String, Object> e : entries) {
                Long txId = asLong(e.get("transactionId"));
                if (txId != null) {
                    entriesByTx.computeIfAbsent(txId, k -> new ArrayList<>()).add(e);
                }
            }
        }
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        if (transactions != null) {
            for (Map<String, Object> t : transactions) {
                List<Map<String, Object>> txEntries =
                        entriesByTx.getOrDefault(asLong(t.get("id")), Collections.emptyList());
                sb.append('\n').append(String.join(",",
                        csvField(dateText(t.get("transactionDate"))),
                        csvField(timeText(t.get("transactionDate"))),
                        csvField(text(t.get("type"))),
                        csvField(text(t.get("categoryName"))),
                        csvField(formatAmount(toAmount(t.get("amount")))),
                        csvField(text(t.get("accountName"))),
                        csvField(text(t.get("description"))),
                        csvField(joinNames(txEntries, "debitName")),
                        csvField(joinNames(txEntries, "creditName")),
                        csvField(text(t.get("externalOrderNo")))));
            }
        }
        return sb.toString();
    }

    /**
     * 生成 CSV 字节（UTF-8 BOM + 文本）
     */
    public byte[] buildCsvBytes(Long userId, List<Map<String, Object>> transactions,
                                List<Map<String, Object>> entries) {
        return ("\uFEFF" + buildCsvText(userId, transactions, entries)).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 金额格式化：两位小数
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /* ────────── JSON 构建 ────────── */

    /**
     * 生成 JSON 备份文本
     */
    public String buildJsonText(Long userId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        root.put("version", 1);
        root.put("accounts", accountsOf(userId));
        root.put("categories", categoriesOf(userId));

        List<Transaction> txs = loadTransactions(userId, true);
        Map<Long, List<Map<String, Object>>> entryRows = loadEntryRows(txs);
        List<Map<String, Object>> transactionNodes = new ArrayList<>();
        List<Map<String, Object>> allEntries = new ArrayList<>();
        for (Transaction t : txs) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", t.getId());
            node.put("date", t.getDate());
            node.put("time", t.getTime());
            node.put("amount", t.getAmount());
            node.put("description", t.getDescription());
            node.put("mode", t.getMode());
            node.put("type", t.getTransactionType());
            node.put("refundStatus", t.getRefundStatus());
            node.put("refundedAmount", t.getRefundedAmount());
            node.put("netAmount", t.getNetAmount());
            node.put("sourceTemplateId", t.getSourceTemplateId());
            node.put("externalOrderNo", t.getExternalOrderNo());
            node.put("importBatchId", t.getImportBatchId());
            node.put("isDeleted", t.getIsDeleted());
            node.put("createdAt", t.getCreatedAt());
            List<Map<String, Object>> nested =
                    entryRows.getOrDefault(t.getId(), Collections.emptyList());
            node.put("entries", nested);
            transactionNodes.add(node);
            allEntries.addAll(nested);
        }
        root.put("transactions", transactionNodes);
        root.put("entries", allEntries);

        Map<String, Object> budgets = new LinkedHashMap<>();
        budgets.put("daily", dailyBudgetsOf(userId));
        budgets.put("special", specialBudgetsOf(userId));
        root.put("budgets", budgets);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("导出 JSON 序列化失败", e);
        }
    }

    /**
     * 下载文件名：duoduocode-ledger-YYYYMMDD.csv / duoduocode-backup-YYYYMMDD.json
     */
    public static String buildFileName(String format, LocalDate date) {
        String day = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        if ("json".equals(format)) {
            return "duoduocode-backup-" + day + ".json";
        }
        return "duoduocode-ledger-" + day + ".csv";
    }

    /* ────────── 数据装载 ────────── */

    private List<Transaction> loadTransactions(Long userId, boolean includeDeleted) {
        if (transactionMapper == null) {
            return new ArrayList<>();
        }
        return includeDeleted
                ? transactionMapper.selectAllByUserIdIncludeDeleted(userId)
                : transactionMapper.selectAllByUserId(userId);
    }

    /**
     * 分录行（含名称）按交易ID分组；交易ID为空集合时跳过查询
     */
    private Map<Long, List<Map<String, Object>>> loadEntryRows(List<Transaction> txs) {
        Map<Long, List<Map<String, Object>>> byTx = new LinkedHashMap<>();
        if (entryMapper == null || txs == null || txs.isEmpty()) {
            return byTx;
        }
        List<Long> ids = new ArrayList<>();
        for (Transaction t : txs) {
            ids.add(t.getId());
        }
        for (Map<String, Object> row : entryMapper.selectExportRowsByTransactionIds(ids)) {
            byTx.computeIfAbsent(asLong(row.get("transactionId")), k -> new ArrayList<>()).add(row);
        }
        return byTx;
    }

    /**
     * CSV 交易行：分类名/账户名由分录推导
     */
    private List<Map<String, Object>> csvTransactionRows(List<Transaction> txs,
                                                         Map<Long, List<Map<String, Object>>> entryRows) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Transaction t : txs) {
            List<Map<String, Object>> entries =
                    entryRows.getOrDefault(t.getId(), Collections.emptyList());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", t.getId());
            row.put("type", t.getTransactionType());
            row.put("amount", t.getAmount());
            row.put("categoryId", idOfType(entries, "category"));
            row.put("categoryName", nameOfType(entries, "category"));
            row.put("accountId", idOfType(entries, "account"));
            row.put("accountName", nameOfType(entries, "account"));
            row.put("description", t.getDescription());
            row.put("transactionDate",
                    t.getDate() == null ? null
                            : t.getDate().atTime(t.getTime() == null ? LocalTime.MIDNIGHT : t.getTime()));
            row.put("externalOrderNo", t.getExternalOrderNo());
            row.put("isDeleted", t.getIsDeleted() != null && t.getIsDeleted() == 1);
            rows.add(row);
        }
        return rows;
    }

    /**
     * CSV 分录行：每交易聚合借/贷两侧名称
     */
    private List<Map<String, Object>> entryPairRows(List<Transaction> txs,
                                                    Map<Long, List<Map<String, Object>>> entryRows) {
        List<Map<String, Object>> pairs = new ArrayList<>();
        for (Transaction t : txs) {
            List<Map<String, Object>> entries =
                    entryRows.getOrDefault(t.getId(), Collections.emptyList());
            List<String> debitNames = new ArrayList<>();
            List<String> creditNames = new ArrayList<>();
            Long debitAccountId = null;
            Long creditAccountId = null;
            for (Map<String, Object> e : entries) {
                if (positive(e.get("debit"))) {
                    if (debitAccountId == null) {
                        debitAccountId = asLong(e.get("accountId"));
                    }
                    addName(debitNames, e.get("name"));
                }
                if (positive(e.get("credit"))) {
                    if (creditAccountId == null) {
                        creditAccountId = asLong(e.get("accountId"));
                    }
                    addName(creditNames, e.get("name"));
                }
            }
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("transactionId", t.getId());
            pair.put("debitAccountId", debitAccountId);
            pair.put("debitName", debitNames.isEmpty() ? null : String.join("+", debitNames));
            pair.put("creditAccountId", creditAccountId);
            pair.put("creditName", creditNames.isEmpty() ? null : String.join("+", creditNames));
            pair.put("amount", t.getAmount());
            pairs.add(pair);
        }
        return pairs;
    }

    private List<Account> accountsOf(Long userId) {
        if (accountMapper == null) {
            return new ArrayList<>();
        }
        return accountMapper.selectByUserId(userId);
    }

    private List<Category> categoriesOf(Long userId) {
        if (categoryMapper == null) {
            return new ArrayList<>();
        }
        return categoryMapper.selectByUserId(userId);
    }

    private List<DailyBudget> dailyBudgetsOf(Long userId) {
        if (dailyBudgetMapper == null) {
            return new ArrayList<>();
        }
        return dailyBudgetMapper.selectAllByUserId(userId);
    }

    private List<SpecialBudget> specialBudgetsOf(Long userId) {
        if (specialBudgetMapper == null) {
            return new ArrayList<>();
        }
        return specialBudgetMapper.selectByUserId(userId);
    }

    /* ────────── 工具 ────────── */

    private static Long idOfType(List<Map<String, Object>> entries, String accountType) {
        for (Map<String, Object> e : entries) {
            if (accountType.equals(e.get("accountType"))) {
                return asLong(e.get("accountId"));
            }
        }
        return null;
    }

    private static String nameOfType(List<Map<String, Object>> entries, String accountType) {
        for (Map<String, Object> e : entries) {
            if (accountType.equals(e.get("accountType"))) {
                Object name = e.get("name");
                return name == null ? null : name.toString();
            }
        }
        return null;
    }

    private static void addName(List<String> names, Object name) {
        if (name != null && !name.toString().isEmpty()) {
            names.add(name.toString());
        }
    }

    private static String joinNames(List<Map<String, Object>> entries, String key) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> e : entries) {
            Object v = e.get(key);
            if (v != null && !v.toString().isEmpty()) {
                names.add(v.toString());
            }
        }
        return String.join("+", names);
    }

    private static String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static String dateText(Object value) {
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate().toString();
        }
        return value == null ? "" : value.toString();
    }

    private static String timeText(Object value) {
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalTime().format(TIME_FORMATTER);
        }
        return value == null ? "" : value.toString();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static BigDecimal toAmount(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }

    private static boolean positive(Object value) {
        BigDecimal amount = toAmount(value);
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private static Long asLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
