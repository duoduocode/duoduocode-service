package com.duoduocode.service.billimport;

import com.duoduocode.service.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账单 CSV 解析器（U01）：编码探测、列名定位、不计收支过滤、非法行、引号转义。
 */
class BillCsvParserTest {

    private final BillCsvParser parser = new BillCsvParser();

    /* ────────── 支付宝样例（UTF-8） ────────── */

    private static final String ALIPAY_CSV = String.join("\n",
            "支付宝交易明细查询",
            "账号:[1111@qq.com]",
            "起始日期:[2026-08-01]  终止日期:[2026-08-03]",
            "---------------------------------交易记录明细列表------------------------------------",
            "交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注",
            "2026-08-03 12:30:00,餐饮美食,美团,美团-午餐,支出,32.00,招商银行(1234),交易成功,2026080322001488x,M001,",
            "2026-08-02 09:00:00,交通出行,滴滴,滴滴打车,支出,45.50,余额宝（含余额）,交易成功,2026080222001489x,M002,",
            "2026-08-01 10:00:00,工资,某公司,八月工资,收入,10000.00,招商银行(1234),交易成功,2026080122001490x,M003,",
            "2026-08-01 11:00:00,投资理财,余额宝,余额宝-转入,不计收支,1000.00,余额宝（含余额）,交易成功,2026080122001491x,M004,"
    );

    @Test
    void 支付宝_utf8_解析() {
        BillCsvParser.ParseResult r = parser.parse("alipay.csv",
                ALIPAY_CSV.getBytes(StandardCharsets.UTF_8));
        assertEquals("alipay", r.getSource());
        // 4 条明细：3 可导入 + 1 不计收支忽略
        assertEquals(3, r.getImportableCount());
        assertEquals(1, r.getIgnoredCount());
        assertEquals(0, r.getDuplicateCount());

        BillCsvParser.Row first = r.getRows().get(0);
        assertEquals("2026-08-03", first.getDate());
        assertEquals("12:30", first.getTime());
        assertEquals("美团-午餐", first.getDescription());
        assertEquals(new java.math.BigDecimal("32.00"), first.getAmount());
        assertEquals("expense", first.getDirection());
        assertEquals("招商银行(1234)", first.getPayMethod());
        assertEquals("2026080322001488x", first.getOrderNo());
        assertEquals("餐饮", first.getCategoryName());
    }

    @Test
    void 支付宝_收入行方向() {
        BillCsvParser.ParseResult r = parser.parse("alipay.csv",
                ALIPAY_CSV.getBytes(StandardCharsets.UTF_8));
        BillCsvParser.Row income = r.getRows().stream()
                .filter(x -> "10000.00".equals(x.getAmount().toPlainString()))
                .findFirst().orElseThrow();
        assertEquals("income", income.getDirection());
    }

    /* ────────── 微信样例（前 16 行说明 + UTF-8） ────────── */

    private static final String WECHAT_CSV = String.join("\n",
            "微信支付账单明细",
            "--------------------",
            "账单时间:[2026-08-01 - 2026-08-03]",
            "",
            "统计时间:2026-08-01 00:00:00 - 2026-08-03 23:59:59",
            "共5笔记录",
            "收入:2笔 100.00元",
            "支出:2笔 67.50元",
            "中性交易:1笔 1000.00元",
            "--------------------",
            "交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注",
            "2026-08-03 12:30:00,商户消费,美团,美团-午餐,支出,¥32.00,招商银行(1234),支付成功,4200011x,S001,",
            "2026-08-02 20:00:00,商户消费,肯德基,肯德基晚餐,支出,¥35.50,零钱,支付成功,4200012x,S002,",
            "2026-08-01 09:00:00,退款,美团,美团退款,收入,¥100.00,招商银行(1234),退款成功,4200013x,S003,",
            "2026-08-01 22:00:00,转账,零钱充值,零钱充值,中性,¥1000.00,零钱,已存入零钱,4200014x,S004,"
    );

    @Test
    void 微信_跳过说明行与中性行() {
        BillCsvParser.ParseResult r = parser.parse("wechat.csv",
                WECHAT_CSV.getBytes(StandardCharsets.UTF_8));
        assertEquals("wechat", r.getSource());
        // 4 条明细：2 支出 + 1 收入 + 1 中性忽略
        assertEquals(3, r.getImportableCount());
        assertEquals(1, r.getIgnoredCount());
        BillCsvParser.Row first = r.getRows().get(0);
        // 微信金额带 ¥ 前缀
        assertEquals(new java.math.BigDecimal("32.00"), first.getAmount());
        assertEquals("美团-午餐", first.getDescription());
        assertEquals("招商银行(1234)", first.getPayMethod());
    }

    /* ────────── 编码探测（F22/U01 核心） ────────── */

    @Test
    void 编码_gbk_探测() {
        BillCsvParser.ParseResult r = parser.parse("alipay.csv",
                ALIPAY_CSV.getBytes(Charset.forName("GBK")));
        assertEquals("alipay", r.getSource());
        assertEquals(3, r.getImportableCount());
        BillCsvParser.Row first = r.getRows().get(0);
        assertEquals("美团-午餐", first.getDescription());
    }

    @Test
    void 编码_utf8_bom_探测() {
        byte[] body = ALIPAY_CSV.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        BillCsvParser.ParseResult r = parser.parse("alipay.csv", withBom);
        assertEquals(3, r.getImportableCount());
        assertEquals("美团-午餐", r.getRows().get(0).getDescription());
    }

    /* ────────── 非法文件（F23） ────────── */

    @Test
    void 非法_非CSV内容抛异常() {
        assertThrows(BusinessException.class, () ->
                parser.parse("x.csv", "不是表头也不像账单的内容随便打打".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void 非法_空文件抛异常() {
        assertThrows(BusinessException.class, () ->
                parser.parse("empty.csv", new byte[0]));
    }

    @Test
    void 非法_列名定位失败抛异常() {
        // 有逗号分隔但无任何已知列名
        assertThrows(BusinessException.class, () ->
                parser.parse("x.csv", "aaa,bbb,ccc\n1,2,3".getBytes(StandardCharsets.UTF_8)));
    }

    /* ────────── 边界行为 ────────── */

    @Test
    void 引号转义_含逗号描述() {
        String csv = String.join("\n",
                "交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注",
                "2026-08-03 12:30:00,餐饮美食,美团,\"午餐, 加饮品\",支出,32.00,招商银行(1234),交易成功,2026080322001488x,M001,"
        );
        BillCsvParser.ParseResult r = parser.parse("alipay.csv", csv.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, r.getImportableCount());
        assertEquals("午餐, 加饮品", r.getRows().get(0).getDescription());
    }

    @Test
    void 非法金额行入ignored() {
        String csv = String.join("\n",
                "交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注",
                "2026-08-03 12:30:00,餐饮美食,美团,美团-午餐,支出,abc,招商银行(1234),交易成功,2026080322001488x,M001,",
                "2026-08-03 13:00:00,餐饮美食,美团,美团-晚餐,支出,15.00,招商银行(1234),交易成功,2026080322001499x,M002,"
        );
        BillCsvParser.ParseResult r = parser.parse("alipay.csv", csv.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, r.getImportableCount());
        assertEquals(1, r.getIgnoredCount());
    }

    @Test
    void 前台预览行数上限() {
        StringBuilder sb = new StringBuilder();
        sb.append("交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注\n");
        for (int i = 0; i < 600; i++) {
            sb.append("2026-08-03 12:30:00,餐饮美食,美团,午餐").append(i)
                    .append(",支出,32.00,招商银行(1234),交易成功,20260803").append(i).append("x,M001,\n");
        }
        BillCsvParser.ParseResult r = parser.parse("alipay.csv", sb.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(600, r.getTotalRows());
        assertEquals(600, r.getImportableCount());
        // 展示明细截断至 500 行，计数不受影响
        assertTrue(r.getRows().size() <= 500);
    }
}
