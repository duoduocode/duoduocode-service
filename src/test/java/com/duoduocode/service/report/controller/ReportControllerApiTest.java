package com.duoduocode.service.report.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class ReportControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long testAccountId;

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);

        Map<String, Object> acctBody = new LinkedHashMap<>();
        acctBody.put("name", "报表测试账户_" + System.currentTimeMillis());
        acctBody.put("type", "asset");
        acctBody.put("icon", "💰");
        acctBody.put("initialBalance", new java.math.BigDecimal("10000.00"));
        JsonNode acctResult = apiPost("/v1/accounts", acctBody);
        assertEquals(0, acctResult.get("code").asInt(), "创建测试账户应成功: " + acctResult);
        testAccountId = acctResult.get("data").asLong();
    }

    private static String devLogin(Long userId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        JsonNode result = executePost(BASE_URL + "/v1/auth/dev-login", body, null);
        assertEquals(0, result.get("code").asInt(), "dev登录应成功: " + result);
        return result.get("data").get("token").asText();
    }

    private static JsonNode apiPost(String path, Object bodyObj) {
        return executePost(BASE_URL + path, bodyObj, authToken);
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

    @Test
    @Order(1)
    void testGetIncomeExpenseReport_shouldReturnData() {
        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/reports/income-expense?startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取收支报表应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(2)
    void testGetAccountTrendReport_shouldReturnData() {
        assertNotNull(testAccountId, "需要先创建账户");

        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/reports/account-trend?accountId=" + testAccountId + "&startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取账户趋势报表应成功: " + result);
    }

    @Test
    @Order(3)
    void testGetCategoryAnalysisReport_shouldReturnData() {
        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/reports/category-analysis?type=expense&startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取分类分析报表应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(4)
    void testGetCategoryAnalysisReport_income_shouldSucceed() {
        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/reports/category-analysis?type=income&startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取收入分类分析应成功: " + result);
    }

    @Test
    @Order(5)
    void testGetMonthlyComparisonReport_shouldReturnData() {
        String month1 = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String month2 = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        JsonNode result = executeGet(BASE_URL + "/v1/reports/monthly-comparison?month1=" + month1 + "&month2=" + month2);

        assertEquals(0, result.get("code").asInt(), "获取月度对比报表应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(6)
    void testGetReport_withoutToken_shouldFail() {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/v1/reports/income-expense?startDate=2024-01-01&endDate=2024-12-31",
                    HttpMethod.GET, entity, String.class);
            JsonNode result = objectMapper.readTree(response.getBody());
            assertNotEquals(0, result.get("code").asInt(), "无Token访问报表应失败");
        } catch (Exception e) {
            assertNotNull(e, "无Token请求应被拒绝");
        }
    }
}
