package com.duoduocode.service.billimport;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 账单 CSV 解析器（纯函数，无 Spring 依赖）。
 * 编码探测（BOM/UTF-8/GBK）→ 列名定位 → 行解析 → 不计收支/非法行过滤。
 */
public class BillCsvParser {

    /** 预览行明细展示上限（计数不受影响） */
    static final int MAX_PREVIEW_ROWS = 500;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 解析账单字节流
     *
     * @param fileName 原始文件名（辅助来源识别）
     * @param content  文件字节
     */
    public ParseResult parse(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件为空");
        }
        String text = decode(content);
        String[] lines = text.split("\r?\n");

        String source = detectSource(fileName, lines);
        int headerIdx = locateHeader(lines);
        if (headerIdx < 0) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNRECOGNIZED, "账单列名定位失败，无法识别文件格式");
        }
        Columns cols = resolveColumns(splitLine(lines[headerIdx]));

        ParseResult result = new ParseResult();
        result.setSource(source);
        int lineNo = headerIdx + 1;
        for (int i = headerIdx + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                lineNo++;
                continue;
            }
            List<String> fields = splitLine(line);
            Row row = buildRow(lineNo, fields, cols);
            if (row != null) {
                result.append(row);
            }
            lineNo++;
        }
        return result;
    }

    // ===== 编码与来源 =====

    /**
     * 编码探测：BOM(UTF-8) → strict UTF-8 → GBK 兜底
     */
    private String decode(byte[] content) {
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xEF
                && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF) {
            return new String(content, 3, content.length - 3, StandardCharsets.UTF_8);
        }
        String utf8 = decodeStrict(content, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return utf8;
        }
        String gbk = decodeStrict(content, Charset.forName("GBK"));
        if (gbk != null) {
            return gbk;
        }
        throw new BusinessException(ResultCode.IMPORT_FILE_INVALID, "导入文件编码无法识别");
    }

    /**
     * 严格解码：含非法字节序列返回 null（String 构造器会静默替换，无法用于探测）
     */
    private String decodeStrict(byte[] content, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private String detectSource(String fileName, String[] lines) {
        for (int i = 0; i < Math.min(lines.length, 20); i++) {
            String line = lines[i];
            if (line.contains("支付宝")) {
                return "alipay";
            }
            if (line.contains("微信")) {
                return "wechat";
            }
        }
        if (fileName != null) {
            String name = fileName.toLowerCase();
            if (name.contains("alipay") || name.contains("支付宝")) {
                return "alipay";
            }
            if (name.contains("wechat") || name.contains("微信")) {
                return "wechat";
            }
        }
        throw new BusinessException(ResultCode.IMPORT_FORMAT_UNRECOGNIZED, "无法识别账单来源（支付宝/微信）");
    }

    /**
     * 列名行定位：同时含「交易时间」「金额」「收/支」的行
     */
    private int locateHeader(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("交易时间") && line.contains("金额") && line.contains("收/支")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 按列名解析列下标（防御式：不依赖固定位置）
     */
    private Columns resolveColumns(List<String> headers) {
        Columns cols = new Columns();
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h.contains("交易时间")) {
                cols.time = i;
            } else if (h.contains("交易分类") || h.contains("交易类型")) {
                cols.category = i;
            } else if (h.contains("对方")) {
                cols.counterparty = i;
            } else if (h.contains("商品")) {
                cols.description = i;
            } else if (h.contains("收/支")) {
                cols.direction = i;
            } else if (h.contains("金额")) {
                cols.amount = i;
            } else if (h.contains("付款方式") || h.contains("支付方式")) {
                cols.payMethod = i;
            } else if (h.contains("交易单号")) {
                cols.orderNo = i;
            } else if (h.contains("状态")) {
                cols.status = i;
            }
        }
        if (cols.time < 0 || cols.direction < 0 || cols.amount < 0) {
            throw new BusinessException(ResultCode.IMPORT_FORMAT_UNRECOGNIZED, "账单列名定位失败，无法识别文件格式");
        }
        return cols;
    }

    // ===== 行解析 =====

    private Row buildRow(int lineNo, List<String> fields, Columns cols) {
        Row row = new Row();
        row.setLineNo(lineNo);
        row.setDate(field(fields, cols.time));
        row.setBillCategory(field(fields, cols.category));
        row.setCounterparty(field(fields, cols.counterparty));
        row.setDescription(field(fields, cols.description));
        row.setPayMethod(field(fields, cols.payMethod));
        row.setOrderNo(field(fields, cols.orderNo));

        String directionText = field(fields, cols.direction);
        if ("支出".equals(directionText)) {
            row.setDirection("expense");
        } else if ("收入".equals(directionText)) {
            row.setDirection("income");
        } else {
            row.setStatus("ignored");
            row.setReason("不计收支");
            row.setDirection(directionText == null || directionText.isEmpty() ? "other" : directionText);
            return row;
        }

        String amountText = normalizeAmount(field(fields, cols.amount));
        BigDecimal amount = parseAmount(amountText);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            row.setStatus("ignored");
            row.setReason("金额非法");
            return row;
        }
        row.setAmount(amount);

        if (!parseDateTime(row, field(fields, cols.time))) {
            row.setStatus("ignored");
            row.setReason("日期非法");
            return row;
        }

        row.setCategoryName(BillCategoryMapper.map(row.getBillCategory()));
        row.setStatus("importable");
        return row;
    }

    private boolean parseDateTime(Row row, String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        String text = raw.trim().replace("T", " ");
        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(text, DATE_TIME);
        } catch (Exception e) {
            try {
                dt = LocalDateTime.parse(text, DATE_TIME_SHORT);
            } catch (Exception e2) {
                return false;
            }
        }
        row.setDate(dt.toLocalDate().toString());
        row.setTime(String.format("%02d:%02d", dt.getHour(), dt.getMinute()));
        return true;
    }

    private String normalizeAmount(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replace("¥", "").replace("￥", "").replace(",", "").replace("+", "");
    }

    private BigDecimal parseAmount(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String field(List<String> fields, int idx) {
        if (idx < 0 || idx >= fields.size()) {
            return "";
        }
        String v = fields.get(idx);
        return v == null ? "" : v.trim();
    }

    /**
     * CSV 行切分：支持引号包裹与 "" 转义
     */
    private List<String> splitLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        fields.add(cur.toString());
        return fields;
    }

    /** 列下标 */
    private static class Columns {
        int time = -1;
        int category = -1;
        int counterparty = -1;
        int description = -1;
        int direction = -1;
        int amount = -1;
        int payMethod = -1;
        int orderNo = -1;
        int status = -1;
    }

    // ===== 结果结构 =====

    /** 解析结果 */
    public static class ParseResult {

        private String source;
        private int totalRows;
        private int importableCount;
        private int duplicateCount;
        private int ignoredCount;
        private List<Row> rows = new ArrayList<>();

        void append(Row row) {
            totalRows++;
            if ("importable".equals(row.getStatus())) {
                importableCount++;
            } else {
                ignoredCount++;
            }
            if (rows.size() < MAX_PREVIEW_ROWS) {
                rows.add(row);
            }
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getImportableCount() {
            return importableCount;
        }

        public void setImportableCount(int importableCount) {
            this.importableCount = importableCount;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }

        public void setDuplicateCount(int duplicateCount) {
            this.duplicateCount = duplicateCount;
        }

        public int getIgnoredCount() {
            return ignoredCount;
        }

        public void setIgnoredCount(int ignoredCount) {
            this.ignoredCount = ignoredCount;
        }

        public List<Row> getRows() {
            return rows;
        }

        public void setRows(List<Row> rows) {
            this.rows = rows;
        }
    }

    /** 账单行 */
    public static class Row {

        private int lineNo;
        private String date;
        private String time;
        private String billCategory;
        private String counterparty;
        private String description;
        private BigDecimal amount;
        private String direction;
        private String categoryName;
        private String payMethod;
        private String accountName;
        private String orderNo;
        private String status;
        private String reason;

        public int getLineNo() {
            return lineNo;
        }

        public void setLineNo(int lineNo) {
            this.lineNo = lineNo;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getBillCategory() {
            return billCategory;
        }

        public void setBillCategory(String billCategory) {
            this.billCategory = billCategory;
        }

        public String getCounterparty() {
            return counterparty;
        }

        public void setCounterparty(String counterparty) {
            this.counterparty = counterparty;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getPayMethod() {
            return payMethod;
        }

        public void setPayMethod(String payMethod) {
            this.payMethod = payMethod;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
