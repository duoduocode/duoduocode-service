package com.duoduocode.service.budget.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class BudgetControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long testCategoryId;
    private static Long specialBudgetId;
    private static final String UNIQUE_SUFFIX = "_" + System.currentTimeMillis();
    private static final String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    private static final String nextMonth = YearMonth.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);

        Map<String, Object> catBody = map(
                "name", "餐饮测试" + UNIQUE_SUFFIX,
                "type", "expense",
                "icon", "🍔",
                "sortOrder", 0
        );
        JsonNode catResult = apiPost("/v1/categories", catBody);
        assertEquals(0, catResult.get("code").asInt(), "创建测试分类应成功: " + catResult);
        testCategoryId = catResult.get("data").asLong();
    }

    private static String devLogin(Long userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        JsonNode result = executePost(BASE_URL + "/v1/auth/dev-login", body, null);
        assertEquals(0, result.get("code").asInt(), "dev登录应成功: " + result);
        return result.get("data").get("token").asText();
    }

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

    private static JsonNode apiPost(String path, Object bodyObj) {
        return executePost(BASE_URL + path, bodyObj, authToken);
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

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    @Test
    @Order(1)
    void testSetDailyBudget_shouldSucceed() {
        Map<String, Object> body = map(
                "categoryId", testCategoryId,
                "monthlyBudget", new BigDecimal("3000.00"),
                "weeklyBudget", new BigDecimal("750.00"),
                "alertThreshold", new BigDecimal("80")
        );
        JsonNode result = apiPut("/v1/budgets/daily", body);

        assertEquals(0, result.get("code").asInt(), "设置日常预算应成功: " + result);
    }

    @Test
    @Order(2)
    void testGetDailyBudget_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/daily?month=" + currentMonth);

        assertEquals(0, result.get("code").asInt(), "获取日常预算应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(3)
    void testGetDailyBudgetUsage_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/daily/usage?month=" + currentMonth);

        assertEquals(0, result.get("code").asInt(), "获取预算使用情况应成功: " + result);
    }

    @Test
    @Order(4)
    void testCalculateCarryoverAmount_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/daily/carryover/calculate?categoryId=" + testCategoryId + "&fromMonth=" + currentMonth);

        assertEquals(0, result.get("code").asInt(), "计算可结转金额应成功: " + result);
    }

    @Test
    @Order(5)
    void testCarryoverBudget_shouldSucceed() {
        Map<String, Object> body = map(
                "categoryId", testCategoryId,
                "fromMonth", currentMonth,
                "toMonth", nextMonth,
                "carryoverAmount", new BigDecimal("500.00")
        );
        JsonNode result = apiPost("/v1/budgets/daily/carryover", body);

        assertEquals(0, result.get("code").asInt(), "执行预算结转应成功: " + result);
    }

    @Test
    @Order(6)
    void testGetCarryoverHistory_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/daily/carryover/history?month=" + currentMonth);

        assertEquals(0, result.get("code").asInt(), "获取结转历史应成功: " + result);
    }

    @Test
    @Order(7)
    void testGetCarryoverStatistics_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/daily/carryover/statistics?month=" + currentMonth);

        assertEquals(0, result.get("code").asInt(), "获取结转统计应成功: " + result);
    }

    @Test
    @Order(8)
    void testCreateSpecialBudget_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "旅游基金" + UNIQUE_SUFFIX,
                "totalAmount", new BigDecimal("10000.00"),
                "startDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "endDate", LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
                "note", "年度旅游预算"
        );
        JsonNode result = apiPost("/v1/budgets/special", body);

        assertEquals(0, result.get("code").asInt(), "创建专项预算应成功: " + result);
        assertNotNull(result.get("data"));
        specialBudgetId = result.get("data").asLong();
    }

    @Test
    @Order(9)
    void testGetSpecialBudgetList_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/special");

        assertEquals(0, result.get("code").asInt(), "获取专项预算列表应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(10)
    void testGetSpecialBudgetDetail_shouldSucceed() {
        assertNotNull(specialBudgetId, "需要先创建专项预算");

        JsonNode result = executeGet(BASE_URL + "/v1/budgets/special/" + specialBudgetId);

        assertEquals(0, result.get("code").asInt(), "获取专项预算详情应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(11)
    void testUpdateSpecialBudget_shouldSucceed() {
        assertNotNull(specialBudgetId, "需要先创建专项预算");

        Map<String, Object> body = map(
                "name", "旅游基金更新" + UNIQUE_SUFFIX,
                "totalAmount", new BigDecimal("15000.00"),
                "startDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "endDate", LocalDate.now().plusMonths(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
                "note", "预算上调"
        );
        JsonNode result = apiPut("/v1/budgets/special/" + specialBudgetId, body);

        assertEquals(0, result.get("code").asInt(), "更新专项预算应成功: " + result);
    }

    @Test
    @Order(12)
    void testGetAvailableSpecialBudgets_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/special/available");

        assertEquals(0, result.get("code").asInt(), "获取可用专项预算应成功: " + result);
    }

    @Test
    @Order(13)
    void testGetSpecialBudgetDetail_nonExistent_shouldFail() {
        JsonNode result = executeGet(BASE_URL + "/v1/budgets/special/999999");

        assertNotEquals(0, result.get("code").asInt(), "不存在的专项预算应返回错误: " + result);
    }

    @Test
    @Order(14)
    void testCompleteSpecialBudget_shouldSucceed() {
        assertNotNull(specialBudgetId, "需要先创建专项预算");

        JsonNode result = apiPost("/v1/budgets/special/" + specialBudgetId + "/complete", new LinkedHashMap<>());

        assertEquals(0, result.get("code").asInt(), "结束专项预算应成功: " + result);
    }
}
