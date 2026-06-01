package com.duoduocode.service.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class AccountControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long createdAccountId;
    private static final String UNIQUE_SUFFIX = "_" + System.currentTimeMillis();

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

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    @Test
    @Order(1)
    void testGetAccountList_shouldReturnList() {
        JsonNode result = executeGet(BASE_URL + "/v1/accounts");

        assertEquals(0, result.get("code").asInt(), "获取账户列表应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(2)
    void testCreateAccount_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "测试储蓄卡" + UNIQUE_SUFFIX,
                "type", "asset",
                "icon", "🏦",
                "color", "#07C160",
                "initialBalance", new BigDecimal("5000.00"),
                "includeInNetWorth", true,
                "allowTransfer", true,
                "sortOrder", 0
        );
        JsonNode result = apiPost("/v1/accounts", body);

        assertEquals(0, result.get("code").asInt(), "创建账户应成功: " + result);
        assertNotNull(result.get("data"));
        createdAccountId = result.get("data").asLong();
        assertTrue(createdAccountId > 0);
    }

    @Test
    @Order(3)
    void testGetAccountDetail_shouldReturnDetail() {
        assertNotNull(createdAccountId, "需要先创建账户");

        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + createdAccountId);

        assertEquals(0, result.get("code").asInt(), "获取账户详情应成功: " + result);
        assertNotNull(result.get("data"));
        assertNotNull(result.get("data").get("name"));
    }

    @Test
    @Order(4)
    void testGetAccountDetail_nonExistent_shouldFail() {
        JsonNode result = executeGet(BASE_URL + "/v1/accounts/999999");

        assertNotEquals(0, result.get("code").asInt(), "不存在的账户应返回错误: " + result);
    }

    @Test
    @Order(5)
    void testUpdateAccount_shouldSucceed() {
        assertNotNull(createdAccountId, "需要先创建账户");

        Map<String, Object> body = map(
                "name", "更新后的储蓄卡" + UNIQUE_SUFFIX,
                "type", "asset",
                "color", "#1989FA"
        );
        JsonNode result = apiPut("/v1/accounts/" + createdAccountId, body);

        assertEquals(0, result.get("code").asInt(), "更新账户应成功: " + result);
    }

    @Test
    @Order(6)
    void testCreateAccount_creditCard_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "测试信用卡" + UNIQUE_SUFFIX,
                "type", "liability",
                "icon", "💳",
                "color", "#FF4444",
                "initialBalance", new BigDecimal("0.00"),
                "includeInNetWorth", true,
                "allowTransfer", false,
                "sortOrder", 1
        );
        JsonNode result = apiPost("/v1/accounts", body);

        assertEquals(0, result.get("code").asInt(), "创建信用卡应成功: " + result);
    }

    @Test
    @Order(7)
    void testGetAccountTransactions_shouldReturnPage() {
        assertNotNull(createdAccountId, "需要先创建账户");

        JsonNode result = executeGet(BASE_URL + "/v1/accounts/" + createdAccountId + "/transactions?page=1&pageSize=10");

        assertEquals(0, result.get("code").asInt(), "获取账户流水应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(8)
    void testAdjustBalance_shouldSucceed() {
        assertNotNull(createdAccountId, "需要先创建账户");

        Map<String, Object> body = map(
                "newBalance", new BigDecimal("8888.88"),
                "reason", "余额初始化调整"
        );
        JsonNode result = apiPost("/v1/accounts/" + createdAccountId + "/adjust-balance", body);

        assertEquals(0, result.get("code").asInt(), "调整余额应成功: " + result);
    }

    @Test
    @Order(9)
    void testCreateAccount_withoutToken_shouldFail() {
        Map<String, Object> body = map(
                "name", "无权限账户",
                "type", "asset"
        );
        JsonNode result = executePost(BASE_URL + "/v1/accounts", body, null);

        assertNotEquals(0, result.get("code").asInt(), "无Token创建账户应失败: " + result);
    }

    @Test
    @Order(10)
    void testDeleteAccount_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "待删除账户" + UNIQUE_SUFFIX,
                "type", "asset",
                "icon", "💰",
                "initialBalance", new BigDecimal("100.00")
        );
        JsonNode createResult = apiPost("/v1/accounts", body);
        assertEquals(0, createResult.get("code").asInt());
        Long deleteId = createResult.get("data").asLong();

        JsonNode result = apiDelete("/v1/accounts/" + deleteId);

        assertEquals(0, result.get("code").asInt(), "删除账户应成功: " + result);
    }

    @Test
    @Order(11)
    void testDeleteAccount_nonExistent_shouldFail() {
        JsonNode result = apiDelete("/v1/accounts/999999");

        assertNotEquals(0, result.get("code").asInt(), "删除不存在的账户应失败: " + result);
    }
}
