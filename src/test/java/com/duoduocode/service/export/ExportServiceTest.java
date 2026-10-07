package com.duoduocode.service.export;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 导出服务（U05）：CSV BOM/列头/借贷列聚合/金额格式、JSON 结构、文件名。
 */
class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    /* ────────── CSV ────────── */

    @Test
    void csv_bom头三字节() {
        byte[] bytes = exportService.buildCsvBytes(ExportFixtures.userId(), ExportFixtures.transactions(), ExportFixtures.entries());
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    void csv_列头含借贷与来源单号() {
        String csv = exportService.buildCsvText(ExportFixtures.userId(), ExportFixtures.transactions(), ExportFixtures.entries());
        String header = csv.split("\n")[0];
        assertTrue(header.contains("借方"));
        assertTrue(header.contains("贷方"));
        assertTrue(header.contains("来源单号"));
        assertTrue(header.contains("日期"));
    }

    @Test
    void csv_行数等于交易数且金额两位小数() {
        String csv = exportService.buildCsvText(ExportFixtures.userId(), ExportFixtures.transactions(), ExportFixtures.entries());
        // 表头 1 行 + 2 笔交易
        String[] lines = csv.split("\n");
        assertEquals(3, lines.length);
        assertTrue(lines[1].contains("32.00"));
    }

    @Test
    void csv_借贷列聚合账户名() {
        String csv = exportService.buildCsvText(ExportFixtures.userId(), ExportFixtures.transactions(), ExportFixtures.entries());
        // 第一笔支出：借=餐饮分类，贷=现金账户（逗号内为分类名/账户名）
        String expenseLine = csv.split("\n")[1];
        assertTrue(expenseLine.contains("餐饮"));
        assertTrue(expenseLine.contains("现金"));
    }

    @Test
    void csv_金额格式化两位小数() {
        assertEquals("32.00", ExportService.formatAmount(new BigDecimal("32")));
        assertEquals("32.50", ExportService.formatAmount(new BigDecimal("32.5")));
        assertEquals("32.57", ExportService.formatAmount(new BigDecimal("32.567")));
        assertEquals("0.00", ExportService.formatAmount(null));
    }

    @Test
    void csv_null入参安全() {
        // transactions/entries 均为 null → 仅表头
        assertEquals("日期,时间,类型,分类,金额,账户,备注,借方,贷方,来源单号",
                exportService.buildCsvText(ExportFixtures.userId(), null, null));
        // entries 为 null → 行内借贷列为空
        String csv = exportService.buildCsvText(ExportFixtures.userId(), ExportFixtures.transactions(), null);
        assertEquals(3, csv.split("\n").length);
    }

    @Test
    void csv_条目缺失交易ID不参与分组() {
        List<Map<String, Object>> entries = new ArrayList<>(ExportFixtures.entries());
        Map<String, Object> orphan = new java.util.HashMap<>();
        orphan.put("debitName", "游离分录");
        entries.add(orphan);
        String csv = exportService.buildCsvText(
                ExportFixtures.userId(), ExportFixtures.transactions(), entries);
        assertFalse(csv.contains("游离分录"), "无交易ID的分录不应出现在借贷列");
    }

    @Test
    void csv_字段含逗号与引号时引号包裹转义() {
        List<Map<String, Object>> txs = new ArrayList<>(ExportFixtures.transactions());
        Map<String, Object> t = new java.util.HashMap<>(txs.get(0));
        t.put("description", "大餐,拼单\"AA\"");
        txs.set(0, t);
        String csv = exportService.buildCsvText(ExportFixtures.userId(), txs, ExportFixtures.entries());
        // 逗号/引号字段需整体加引号且内部引号翻倍
        assertTrue(csv.contains("\"大餐,拼单\"\"AA\"\"\""), "CSV 转义不符合 RFC 4180: " + csv);
    }

    /* ────────── JSON ────────── */

    @Test
    void json_结构五键齐全() {
        String json = exportService.buildJsonText(ExportFixtures.userId());
        assertTrue(json.contains("\"accounts\""));
        assertTrue(json.contains("\"categories\""));
        assertTrue(json.contains("\"transactions\""));
        assertTrue(json.contains("\"entries\""));
        assertTrue(json.contains("\"budgets\""));
        assertTrue(json.contains("\"exportedAt\""));
        assertTrue(json.contains("\"version\""));
    }

    /* ────────── 文件名 ────────── */

    @Test
    void 文件名_含日期() {
        String name = ExportService.buildFileName("csv", LocalDate.of(2026, 8, 20));
        assertEquals("duoduocode-ledger-20260820.csv", name);
        assertEquals("duoduocode-backup-20260820.json",
                ExportService.buildFileName("json", LocalDate.of(2026, 8, 20)));
    }
}
