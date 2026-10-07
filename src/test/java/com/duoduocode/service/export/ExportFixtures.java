package com.duoduocode.service.export;

import com.duoduocode.service.transaction.dto.TransactionDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 导出测试数据构造（U05）。 */
final class ExportFixtures {

    private ExportFixtures() {
    }

    static Long userId() {
        return 990001L;
    }

    static List<Map<String, Object>> transactions() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> t1 = new HashMap<>();
        t1.put("id", 101L);
        t1.put("type", "expense");
        t1.put("amount", new BigDecimal("32.00"));
        t1.put("categoryId", 10L);
        t1.put("categoryName", "餐饮");
        t1.put("accountId", 3L);
        t1.put("accountName", "现金");
        t1.put("description", "美团-午餐");
        t1.put("transactionDate", LocalDateTime.of(2026, 8, 3, 12, 30));
        t1.put("externalOrderNo", "2026080322001488x");
        t1.put("isDeleted", false);
        list.add(t1);

        Map<String, Object> t2 = new HashMap<>();
        t2.put("id", 102L);
        t2.put("type", "income");
        t2.put("amount", new BigDecimal("10000.00"));
        t2.put("categoryId", 20L);
        t2.put("categoryName", "工资");
        t2.put("accountId", 3L);
        t2.put("accountName", "现金");
        t2.put("description", "八月工资");
        t2.put("transactionDate", LocalDateTime.of(2026, 8, 1, 10, 0));
        t2.put("externalOrderNo", null);
        t2.put("isDeleted", false);
        list.add(t2);
        return list;
    }

    static List<Map<String, Object>> entries() {
        List<Map<String, Object>> list = new ArrayList<>();
        // t101 支出：借 餐饮 / 贷 现金
        list.add(entry(101L, 10L, "餐饮", 3L, "现金", new BigDecimal("32.00")));
        // t102 收入：借 现金 / 贷 工资
        list.add(entry(102L, 3L, "现金", 20L, "工资", new BigDecimal("10000.00")));
        return list;
    }

    private static Map<String, Object> entry(Long txId, Long debitId, String debitName,
                                             Long creditId, String creditName, BigDecimal amount) {
        Map<String, Object> e = new HashMap<>();
        e.put("transactionId", txId);
        e.put("debitAccountId", debitId);
        e.put("debitName", debitName);
        e.put("creditAccountId", creditId);
        e.put("creditName", creditName);
        e.put("amount", amount);
        return e;
    }

    static TransactionDTO dto(Long id, String type, BigDecimal amount) {
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionType(type);
        dto.setAmount(amount);
        return dto;
    }
}
