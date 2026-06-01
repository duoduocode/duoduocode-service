package com.duoduocode.service.recurring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class RecurringTemplateControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long testAccountId;
    private static Long testCategoryId;
    private static Long createdTemplateId;
    private static final String UNIQUE_SUFFIX = "_" + System.currentTimeMillis();

    @BeforeAll
    static void setUp() {
        authToken = devLogin(1L);

        Map<String, Object> acctBody = map(
                "name", "周期测试账户" + UNIQUE_SUFFIX,
                "type", "asset",
                "icon", "💰",
                "initialBalance", new BigDecimal("5000.00")
        );
        JsonNode acctResult = apiPost("/v1/accounts", acctBody);
        assertEquals(0, acctResult.get("code").asInt(), "创建测试账户应成功: " + acctResult);
        testAccountId = acctResult.get("data").asLong();

        Map<String, Object> catBody = map(
                "name", "订阅测试" + UNIQUE_SUFFIX,
                "type", "expense",
                "icon", "📱"
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
    void testCreateTemplate_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "月度订阅" + UNIQUE_SUFFIX,
                "type", "expense",
                "amount", new BigDecimal("99.00"),
                "categoryId", testCategoryId,
                "accountId", testAccountId,
                "description", "每月订阅服务",
                "frequency", "monthly",
                "dayOfMonth", 15,
                "startDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        JsonNode result = apiPost("/v1/recurring-templates", body);

        assertEquals(0, result.get("code").asInt(), "创建周期模板应成功: " + result);
        assertNotNull(result.get("data"));
        createdTemplateId = result.get("data").asLong();
        assertTrue(createdTemplateId > 0);
    }

    @Test
    @Order(2)
    void testGetTemplateList_shouldReturnList() {
        JsonNode result = executeGet(BASE_URL + "/v1/recurring-templates");

        assertEquals(0, result.get("code").asInt(), "获取模板列表应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(3)
    void testGetTemplateDetail_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        JsonNode result = executeGet(BASE_URL + "/v1/recurring-templates/" + createdTemplateId);

        assertEquals(0, result.get("code").asInt(), "获取模板详情应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(4)
    void testUpdateTemplate_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        Map<String, Object> body = map(
                "name", "月度订阅更新" + UNIQUE_SUFFIX,
                "type", "expense",
                "amount", new BigDecimal("129.00"),
                "categoryId", testCategoryId,
                "accountId", testAccountId,
                "description", "更新后的订阅描述",
                "frequency", "monthly",
                "dayOfMonth", 20,
                "startDate", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
        JsonNode result = apiPut("/v1/recurring-templates/" + createdTemplateId, body);

        assertEquals(0, result.get("code").asInt(), "更新模板应成功: " + result);
    }

    @Test
    @Order(5)
    void testGetDueTemplates_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/recurring-templates/due");

        assertEquals(0, result.get("code").asInt(), "获取到期提醒列表应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(6)
    void testPauseTemplate_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        JsonNode result = apiPost("/v1/recurring-templates/" + createdTemplateId + "/pause", new LinkedHashMap<>());

        assertEquals(0, result.get("code").asInt(), "暂停模板应成功: " + result);
    }

    @Test
    @Order(7)
    void testResumeTemplate_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        JsonNode result = apiPost("/v1/recurring-templates/" + createdTemplateId + "/resume", new LinkedHashMap<>());

        assertEquals(0, result.get("code").asInt(), "恢复模板应成功: " + result);
    }

    @Test
    @Order(8)
    void testTriggerTemplate_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        JsonNode result = apiPost("/v1/recurring-templates/" + createdTemplateId + "/trigger", new LinkedHashMap<>());

        assertEquals(0, result.get("code").asInt(), "触发模板提醒应成功: " + result);
    }

    @Test
    @Order(9)
    void testGetTemplateDetail_nonExistent_shouldFail() {
        JsonNode result = executeGet(BASE_URL + "/v1/recurring-templates/999999");

        assertNotEquals(0, result.get("code").asInt(), "不存在的模板应返回错误: " + result);
    }

    @Test
    @Order(10)
    void testDeleteTemplate_shouldSucceed() {
        assertNotNull(createdTemplateId, "需要先创建模板");

        JsonNode result = apiDelete("/v1/recurring-templates/" + createdTemplateId);

        assertEquals(0, result.get("code").asInt(), "删除模板应成功: " + result);
    }

    @Test
    @Order(11)
    void testDeleteTemplate_nonExistent_shouldFail() {
        JsonNode result = apiDelete("/v1/recurring-templates/999999");

        assertNotEquals(0, result.get("code").asInt(), "删除不存在的模板应失败: " + result);
    }
}
