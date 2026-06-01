package com.duoduocode.service.investment.controller;

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
class InvestmentControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long testAccountId;
    private static final String UNIQUE_SUFFIX = "_" + System.currentTimeMillis();

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);

        Map<String, Object> acctBody = map(
                "name", "投资理财账户" + UNIQUE_SUFFIX,
                "type", "asset",
                "icon", "📈",
                "initialBalance", new BigDecimal("100000.00")
        );
        JsonNode acctResult = apiPost("/v1/accounts", acctBody);
        assertEquals(0, acctResult.get("code").asInt(), "创建投资账户应成功: " + acctResult);
        testAccountId = acctResult.get("data").asLong();
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

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    @Test
    @Order(1)
    void testRecordMarketValue_shouldSucceed() {
        Map<String, Object> body = map(
                "date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "marketValue", new BigDecimal("105000.00"),
                "costBasis", new BigDecimal("100000.00"),
                "note", "今日市值快照"
        );
        JsonNode result = apiPost("/v1/accounts/" + testAccountId + "/market-value", body);

        assertEquals(0, result.get("code").asInt(), "记录市值应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").asLong() > 0);
    }

    @Test
    @Order(2)
    void testGetMarketValueHistory_shouldSucceed() {
        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/market-value/history?startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取市值历史应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(3)
    void testGetLatestMarketValue_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/market-value/latest");

        assertEquals(0, result.get("code").asInt(), "获取最新市值应成功: " + result);
    }

    @Test
    @Order(4)
    void testCalculateProfit_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/market-value/profit");

        assertEquals(0, result.get("code").asInt(), "计算收益应成功: " + result);
    }

    @Test
    @Order(5)
    void testRecordIncome_shouldSucceed() {
        Map<String, Object> body = map(
                "date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "amount", new BigDecimal("500.00"),
                "type", "dividend",
                "note", "季度分红",
                "isReinvested", 0
        );
        JsonNode result = apiPost("/v1/accounts/" + testAccountId + "/income", body);

        assertEquals(0, result.get("code").asInt(), "记录收益应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").asLong() > 0);
    }

    @Test
    @Order(6)
    void testRecordIncome_dailyType_shouldSucceed() {
        Map<String, Object> body = map(
                "date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                "amount", new BigDecimal("10.00"),
                "type", "daily",
                "note", "每日收益",
                "isReinvested", 1
        );
        JsonNode result = apiPost("/v1/accounts/" + testAccountId + "/income", body);

        assertEquals(0, result.get("code").asInt(), "记录每日收益应成功: " + result);
    }

    @Test
    @Order(7)
    void testGetIncomeHistory_shouldSucceed() {
        String startDate = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/income/history?startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取收益历史应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(8)
    void testGetMonthlyIncome_shouldSucceed() {
        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/income/monthly?month=" + month);

        assertEquals(0, result.get("code").asInt(), "获取月度收益应成功: " + result);
    }

    @Test
    @Order(9)
    void testGetTotalIncome_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + testAccountId + "/income/total");

        assertEquals(0, result.get("code").asInt(), "获取总收益应成功: " + result);
    }
}
