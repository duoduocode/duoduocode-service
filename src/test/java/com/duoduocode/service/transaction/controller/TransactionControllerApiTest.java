package com.duoduocode.service.transaction.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class TransactionControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static String authToken;
    private static Long testAccountId;
    private static Long testExpenseCategoryId;
    private static Long testIncomeCategoryId;
    private static Long testTargetAccountId;
    private static Long createdTransactionId;

    private static final String UNIQUE_SUFFIX = "_" + System.currentTimeMillis();

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);

        testAccountId = createTestAccount("测试银行账户" + UNIQUE_SUFFIX, "asset", new BigDecimal("10000.00"));
        testTargetAccountId = createTestAccount("测试现金账户" + UNIQUE_SUFFIX, "asset", new BigDecimal("5000.00"));
        createTestAccount("测试信用卡" + UNIQUE_SUFFIX, "liability", new BigDecimal("0.00"));

        testExpenseCategoryId = createTestCategory("餐饮" + UNIQUE_SUFFIX, "expense", "🍔");
        testIncomeCategoryId = createTestCategory("工资" + UNIQUE_SUFFIX, "income", "💰");
    }

    // ==================== 认证辅助方法 ====================

    private static String devLogin(Long userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        JsonNode result = executePost(BASE_URL + "/v1/auth/dev-login", body, null);
        assertEquals(0, result.get("code").asInt(), "dev登录应返回code=0, 实际响应: " + result);
        return result.get("data").get("token").asText();
    }

    private static Long createTestAccount(String name, String type, BigDecimal initialBalance) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("icon", "💰");
        body.put("color", "#07C160");
        body.put("initialBalance", initialBalance);
        body.put("includeInNetWorth", true);
        body.put("allowTransfer", true);
        body.put("sortOrder", 0);

        JsonNode result = executePost(BASE_URL + "/v1/accounts", body, authToken);
        assertEquals(0, result.get("code").asInt(), "创建账户应返回code=0, 实际响应: " + result);
        return result.get("data").asLong();
    }

    private static Long createTestCategory(String name, String type, String icon) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("icon", icon);
        body.put("sortOrder", 0);

        JsonNode result = executePost(BASE_URL + "/v1/categories", body, authToken);
        assertEquals(0, result.get("code").asInt(), "创建分类应返回code=0, 实际响应: " + result);
        return result.get("data").asLong();
    }

    // ==================== HTTP 请求辅助方法 ====================

    private static JsonNode executeGet(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("GET " + url + " failed", e);
        }
    }

    private static JsonNode executePost(String url, Object bodyObj, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null) {
                headers.set("Authorization", "Bearer " + token);
            }
            String jsonBody = objectMapper.writeValueAsString(bodyObj);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("POST " + url + " failed", e);
        }
    }

    private static JsonNode apiPost(String path, Object bodyObj) {
        return executePost(BASE_URL + path, bodyObj, authToken);
    }

    private static JsonNode apiPut(String path, Object bodyObj) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + authToken);
            String jsonBody = objectMapper.writeValueAsString(bodyObj);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL + path, HttpMethod.PUT, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("PUT " + path + " failed", e);
        }
    }

    private static JsonNode apiDelete(String path) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL + path, HttpMethod.DELETE, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("DELETE " + path + " failed", e);
        }
    }

    // ==================== 数据构建辅助方法 ====================

    private static Map<String, Object> createTransactionBody(String type, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionType", type);
        body.put("amount", amount);
        body.put("date", LocalDate.now().format(dateFmt));
        body.put("time", LocalTime.now().format(timeFmt));
        body.put("description", type + "测试交易_" + System.currentTimeMillis());
        body.put("accountId", testAccountId);
        if ("transfer".equals(type) || "repayment".equals(type)) {
            body.put("targetAccountId", testTargetAccountId);
        }
        if ("expense".equals(type) || "income".equals(type)) {
            body.put("categoryId", "expense".equals(type) ? testExpenseCategoryId : testIncomeCategoryId);
        }
        body.put("mode", "simple");
        return body;
    }

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    // ==================== 1. 创建交易 POST /v1/transactions ====================

    @Test
    @Order(1)
    void testCreateTransaction_expense_shouldSucceed() {
        Map<String, Object> body = createTransactionBody("expense", new BigDecimal("88.50"));
        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "创建支出交易应成功: " + result);
        assertNotNull(result.get("data"));
        createdTransactionId = result.get("data").asLong();
        assertTrue(createdTransactionId > 0);
    }

    @Test
    @Order(2)
    void testCreateTransaction_income_shouldSucceed() {
        Map<String, Object> body = createTransactionBody("income", new BigDecimal("5000.00"));
        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "创建收入交易应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").asLong() > 0);
    }

    @Test
    @Order(3)
    void testCreateTransaction_transfer_shouldSucceed() {
        Map<String, Object> body = createTransactionBody("transfer", new BigDecimal("500.00"));
        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "创建转账交易应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").asLong() > 0);
    }

    @Test
    @Order(4)
    void testCreateTransaction_repayment_shouldSucceed() {
        Map<String, Object> body = createTransactionBody("repayment", new BigDecimal("2000.00"));
        body.put("targetAccountId", testTargetAccountId);
        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "创建还款交易应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").asLong() > 0);
    }

    @Test
    @Order(5)
    void testCreateTransaction_missingAmount_shouldFail() {
        Map<String, Object> body = map(
                "transactionType", "expense",
                "accountId", testAccountId,
                "categoryId", testExpenseCategoryId,
                "mode", "simple"
        );
        JsonNode result = apiPost("/v1/transactions", body);

        assertNotEquals(0, result.get("code").asInt(), "缺少金额应返回错误: " + result);
    }

    @Test
    @Order(6)
    void testCreateTransaction_withoutToken_shouldReturnUnauthorized() {
        Map<String, Object> body = createTransactionBody("expense", new BigDecimal("50.00"));
        JsonNode result = executePost(BASE_URL + "/v1/transactions", body, null);

        assertNotEquals(0, result.get("code").asInt(), "无Token应被拦截: " + result);
    }

    // ==================== 2. 获取交易列表 GET /v1/transactions ====================

    @Test
    @Order(7)
    void testGetTransactionList_shouldReturnPageResult() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions?page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "获取交易列表应成功: " + result);
        assertNotNull(result.get("data"));
        assertNotNull(result.get("data").get("list"));
        assertTrue(result.get("data").get("total").asLong() >= 0);
    }

    @Test
    @Order(8)
    void testGetTransactionList_withDateFilter_shouldSucceed() {
        String lastMonth = LocalDate.now().minusMonths(1).format(dateFmt);
        String nextMonth = LocalDate.now().plusMonths(1).format(dateFmt);

        JsonNode result = executeGet(BASE_URL + "/v1/transactions?page=1&pageSize=10&startDate=" + lastMonth + "&endDate=" + nextMonth);

        assertEquals(0, result.get("code").asInt(), "带日期过滤的交易列表应成功: " + result);
    }

    @Test
    @Order(9)
    void testGetTransactionList_withNoMatchDate_shouldReturnEmpty() {
        String farPast = LocalDate.now().minusYears(10).format(dateFmt);
        String farPastEnd = LocalDate.now().minusYears(9).format(dateFmt);

        JsonNode result = executeGet(BASE_URL + "/v1/transactions?page=1&pageSize=10&startDate=" + farPast + "&endDate=" + farPastEnd);

        assertEquals(0, result.get("code").asInt());
        assertEquals(0, result.get("data").get("total").asLong(), "无匹配数据应返回total=0");
    }

    // ==================== 3. 获取最近交易 GET /v1/transactions/recent ====================

    @Test
    @Order(10)
    void testGetRecentTransactions_shouldReturnList() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/recent?limit=5");

        assertEquals(0, result.get("code").asInt(), "获取最近交易应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(11)
    void testGetRecentTransactions_defaultLimit_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/recent");

        assertEquals(0, result.get("code").asInt(), "默认limit获取最近交易应成功: " + result);
    }

    @Test
    @Order(12)
    void testGetRecentTransactions_limitOne_shouldReturnAtMostOne() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/recent?limit=1");

        assertEquals(0, result.get("code").asInt());
        assertTrue(result.get("data").size() <= 1, "limit=1应返回至多1条");
    }

    // ==================== 4. 获取交易详情 GET /v1/transactions/{id} ====================

    @Test
    @Order(13)
    void testGetTransactionDetail_shouldReturnDetail() {
        assertNotNull(createdTransactionId, "需要先创建交易");

        JsonNode result = executeGet(BASE_URL + "/v1/transactions/" + createdTransactionId);

        assertEquals(0, result.get("code").asInt(), "获取交易详情应成功: " + result);
        assertNotNull(result.get("data"));
        assertEquals(createdTransactionId.longValue(), result.get("data").get("id").asLong());
        assertNotNull(result.get("data").get("amount"));
        assertNotNull(result.get("data").get("transactionType"));
        assertNotNull(result.get("data").get("entries"));
    }

    @Test
    @Order(14)
    void testGetTransactionDetail_nonExistent_shouldReturnNotFound() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/999999");

        assertNotEquals(0, result.get("code").asInt(), "不存在的交易应返回错误: " + result);
    }

    // ==================== 5. 更新交易 PUT /v1/transactions/{id} ====================

    @Test
    @Order(15)
    void testUpdateTransaction_shouldSucceed() {
        assertNotNull(createdTransactionId, "需要先创建交易");

        Map<String, Object> body = createTransactionBody("expense", new BigDecimal("120.00"));
        body.put("description", "更新后的交易描述_" + System.currentTimeMillis());
        JsonNode result = apiPut("/v1/transactions/" + createdTransactionId, body);

        assertEquals(0, result.get("code").asInt(), "更新交易应成功: " + result);

        JsonNode detail = executeGet(BASE_URL + "/v1/transactions/" + createdTransactionId);
        assertEquals(0, detail.get("code").asInt());
        assertEquals(new BigDecimal("120.00"), new BigDecimal(detail.get("data").get("amount").asText()));
    }

    @Test
    @Order(16)
    void testUpdateTransaction_nonExistent_shouldFail() {
        Map<String, Object> body = createTransactionBody("expense", new BigDecimal("50.00"));
        JsonNode result = apiPut("/v1/transactions/999999", body);

        assertNotEquals(0, result.get("code").asInt(), "更新不存在的交易应返回错误: " + result);
    }

    // ==================== 6. 搜索筛选交易 GET /v1/transactions/search ====================

    @Test
    @Order(17)
    void testSearchTransactions_byKeyword_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?keyword=测试&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按关键词搜索应成功: " + result);
        assertNotNull(result.get("data").get("list"));
    }

    @Test
    @Order(18)
    void testSearchTransactions_byType_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?type=expense&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按类型搜索应成功: " + result);
    }

    @Test
    @Order(19)
    void testSearchTransactions_byAmountRange_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?minAmount=10&maxAmount=10000&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按金额范围搜索应成功: " + result);
    }

    @Test
    @Order(20)
    void testSearchTransactions_byAccountId_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?accountId=" + testAccountId + "&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按账户搜索应成功: " + result);
    }

    @Test
    @Order(21)
    void testSearchTransactions_byCategoryId_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?categoryId=" + testExpenseCategoryId + "&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按分类搜索应成功: " + result);
    }

    @Test
    @Order(22)
    void testSearchTransactions_combinedFilter_shouldSucceed() {
        String today = LocalDate.now().format(dateFmt);
        String nextMonth = LocalDate.now().plusMonths(1).format(dateFmt);

        String url = BASE_URL + "/v1/transactions/search"
                + "?keyword=测试"
                + "&type=expense"
                + "&categoryId=" + testExpenseCategoryId
                + "&startDate=" + today
                + "&endDate=" + nextMonth
                + "&minAmount=1"
                + "&maxAmount=99999"
                + "&page=1&pageSize=10"
                + "&sortBy=amount&sortOrder=desc";

        JsonNode result = executeGet(url);

        assertEquals(0, result.get("code").asInt(), "组合条件搜索应成功: " + result);
    }

    @Test
    @Order(23)
    void testSearchTransactions_noMatch_shouldReturnEmpty() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?keyword=不存在的交易XYZABC123&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt());
        assertEquals(0, result.get("data").get("total").asLong(), "无匹配应返回total=0");
    }

    @Test
    @Order(24)
    void testSearchTransactions_sortByDateAsc_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?sortBy=date&sortOrder=asc&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按日期升序搜索应成功: " + result);
    }

    @Test
    @Order(25)
    void testSearchTransactions_sortByAmountAsc_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions/search?sortBy=amount&sortOrder=asc&page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "按金额升序搜索应成功: " + result);
    }

    // ==================== 7. 重复交易检测 POST /v1/transactions/check-duplicate ====================

    @Test
    @Order(26)
    void testCheckDuplicate_withExistingParameters_shouldCheck() {
        Map<String, Object> body = map(
                "amount", new BigDecimal("88.50"),
                "date", LocalDate.now().format(dateFmt),
                "description", "支出测试交易"
        );
        JsonNode result = apiPost("/v1/transactions/check-duplicate", body);

        assertEquals(0, result.get("code").asInt(), "重复检测应成功: " + result);
        assertNotNull(result.get("data"));
        assertNotNull(result.get("data").get("isDuplicate"));
    }

    @Test
    @Order(27)
    void testCheckDuplicate_withUniqueParameters_shouldReturnNotDuplicate() {
        Map<String, Object> body = map(
                "amount", new BigDecimal("999999.99"),
                "date", LocalDate.now().format(dateFmt),
                "description", "绝对不会重复的交易描述_" + System.currentTimeMillis()
        );
        JsonNode result = apiPost("/v1/transactions/check-duplicate", body);

        assertEquals(0, result.get("code").asInt(), "唯一数据检测应成功: " + result);
    }

    // ==================== 8. 退款处理 POST /v1/transactions/{id}/refund ====================

    @Test
    @Order(28)
    void testRefundTransaction_partial_shouldSucceed() {
        Map<String, Object> createBody = createTransactionBody("expense", new BigDecimal("200.00"));
        JsonNode createResult = apiPost("/v1/transactions", createBody);
        assertEquals(0, createResult.get("code").asInt(), "创建待退款交易应成功: " + createResult);
        Long refundTxId = createResult.get("data").asLong();

        Map<String, Object> refundBody = map(
                "amount", new BigDecimal("50.00"),
                "date", LocalDate.now().format(dateFmt),
                "description", "部分退款测试",
                "accountId", testAccountId
        );
        JsonNode result = apiPost("/v1/transactions/" + refundTxId + "/refund", refundBody);

        assertEquals(0, result.get("code").asInt(), "部分退款应成功: " + result);

        JsonNode detail = executeGet(BASE_URL + "/v1/transactions/" + refundTxId);
        assertEquals(0, detail.get("code").asInt());
        assertTrue(detail.get("data").has("refundedAmount"), "退款后应有refundedAmount字段");
    }

    @Test
    @Order(29)
    void testRefundTransaction_full_shouldSucceed() {
        Map<String, Object> createBody = createTransactionBody("expense", new BigDecimal("100.00"));
        JsonNode createResult = apiPost("/v1/transactions", createBody);
        assertEquals(0, createResult.get("code").asInt(), "创建待全额退款交易应成功: " + createResult);
        Long refundTxId = createResult.get("data").asLong();

        Map<String, Object> refundBody = map(
                "amount", new BigDecimal("100.00"),
                "date", LocalDate.now().format(dateFmt),
                "description", "全额退款测试",
                "accountId", testAccountId
        );
        JsonNode result = apiPost("/v1/transactions/" + refundTxId + "/refund", refundBody);

        assertEquals(0, result.get("code").asInt(), "全额退款应成功: " + result);
    }

    @Test
    @Order(30)
    void testRefundTransaction_amountExceeds_shouldFail() {
        Map<String, Object> createBody = createTransactionBody("expense", new BigDecimal("50.00"));
        JsonNode createResult = apiPost("/v1/transactions", createBody);
        assertEquals(0, createResult.get("code").asInt(), "创建待退款交易应成功: " + createResult);
        Long refundTxId = createResult.get("data").asLong();

        Map<String, Object> refundBody = map(
                "amount", new BigDecimal("100.00"),
                "date", LocalDate.now().format(dateFmt),
                "description", "超额退款应失败",
                "accountId", testAccountId
        );
        JsonNode result = apiPost("/v1/transactions/" + refundTxId + "/refund", refundBody);

        assertNotEquals(0, result.get("code").asInt(), "退款金额超过原交易金额应返回错误: " + result);
    }

    @Test
    @Order(31)
    void testRefundTransaction_nonExistent_shouldFail() {
        Map<String, Object> refundBody = map(
                "amount", new BigDecimal("10.00"),
                "date", LocalDate.now().format(dateFmt),
                "description", "退款不存在的交易",
                "accountId", testAccountId
        );
        JsonNode result = apiPost("/v1/transactions/999999/refund", refundBody);

        assertNotEquals(0, result.get("code").asInt(), "退款不存在的交易应返回错误: " + result);
    }

    // ==================== 9. 删除交易 DELETE /v1/transactions/{id} ====================

    @Test
    @Order(32)
    void testDeleteTransaction_shouldSucceed() {
        Map<String, Object> createBody = createTransactionBody("expense", new BigDecimal("30.00"));
        JsonNode createResult = apiPost("/v1/transactions", createBody);
        assertEquals(0, createResult.get("code").asInt());
        Long deleteTxId = createResult.get("data").asLong();

        JsonNode result = apiDelete("/v1/transactions/" + deleteTxId);

        assertEquals(0, result.get("code").asInt(), "删除交易应成功: " + result);

        JsonNode detail = executeGet(BASE_URL + "/v1/transactions/" + deleteTxId);
        assertNotEquals(0, detail.get("code").asInt(), "已删除交易查询应返回错误");
    }

    @Test
    @Order(33)
    void testDeleteTransaction_nonExistent_shouldFail() {
        JsonNode result = apiDelete("/v1/transactions/999999");

        assertNotEquals(0, result.get("code").asInt(), "删除不存在的交易应返回错误: " + result);
    }

    // ==================== 10. 边界与数据完整性测试 ====================

    @Test
    @Order(34)
    void testCreateTransaction_largeAmount_shouldSucceed() {
        Map<String, Object> body = createTransactionBody("income", new BigDecimal("9999999999.99"));
        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "大宗交易应成功: " + result);
    }

    @Test
    @Order(35)
    void testCreateTransaction_withFullFields_shouldSucceed() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionType", "expense");
        body.put("amount", new BigDecimal("156.78"));
        body.put("date", LocalDate.now().format(dateFmt));
        body.put("time", LocalTime.of(18, 30, 0).format(timeFmt));
        body.put("description", "完整字段测试 - 超市购物");
        body.put("accountId", testAccountId);
        body.put("categoryId", testExpenseCategoryId);
        body.put("mode", "simple");

        JsonNode result = apiPost("/v1/transactions", body);

        assertEquals(0, result.get("code").asInt(), "完整字段创建应成功: " + result);
    }

    @Test
    @Order(36)
    void testGetTransactionList_pagination_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/transactions?page=2&pageSize=2");

        assertEquals(0, result.get("code").asInt());
        assertNotNull(result.get("data").get("list"));
        assertNotNull(result.get("data").get("total"));
        assertTrue(result.get("data").get("list").size() <= 2, "分页结果应不超过pageSize");
    }
}
