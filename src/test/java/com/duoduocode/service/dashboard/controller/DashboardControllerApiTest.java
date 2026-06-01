package com.duoduocode.service.dashboard.controller;

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
class DashboardControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);
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
    void testGetDashboard_shouldReturnData() {
        JsonNode result = executeGet(BASE_URL + "/v1/dashboard");

        assertEquals(0, result.get("code").asInt(), "获取首页看板应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(2)
    void testGetDashboardSummary_shouldReturnData() {
        JsonNode result = executeGet(BASE_URL + "/v1/dashboard/summary");

        assertEquals(0, result.get("code").asInt(), "获取看板摘要应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(3)
    void testGetNetWorthTrend_shouldReturnData() {
        String startDate = LocalDate.now().minusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        JsonNode result = executeGet(BASE_URL + "/v1/dashboard/net-worth-trend?startDate=" + startDate + "&endDate=" + endDate);

        assertEquals(0, result.get("code").asInt(), "获取净资产趋势应成功: " + result);
    }

    @Test
    @Order(4)
    void testGetMonthlyDetail_shouldReturnData() {
        String month = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        JsonNode result = executeGet(BASE_URL + "/v1/dashboard/monthly?month=" + month);

        assertEquals(0, result.get("code").asInt(), "获取月度详情应成功: " + result);
    }

    @Test
    @Order(5)
    void testGetMonthlyDetail_defaultMonth_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/dashboard/monthly");

        assertEquals(0, result.get("code").asInt(), "默认月份获取月度详情应成功: " + result);
    }

    @Test
    @Order(6)
    void testGetTodayData_shouldReturnData() {
        JsonNode result = executeGet(BASE_URL + "/v1/dashboard/today");

        assertEquals(0, result.get("code").asInt(), "获取今日数据应成功: " + result);
        assertNotNull(result.get("data"));
        assertTrue(result.get("data").has("date"));
        assertTrue(result.get("data").has("expense"));
    }
}
