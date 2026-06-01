package com.duoduocode.service.category.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class CategoryControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
    private static Long createdCategoryId;
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
    void testGetCategoryList_shouldReturnTree() {
        JsonNode result = executeGet(BASE_URL + "/v1/categories");

        assertEquals(0, result.get("code").asInt(), "获取分类列表应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(2)
    void testGetCategoryList_byType_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/categories?type=expense");

        assertEquals(0, result.get("code").asInt(), "按类型获取分类应成功: " + result);
    }

    @Test
    @Order(3)
    void testCreateCategory_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "测试餐饮分类" + UNIQUE_SUFFIX,
                "type", "expense",
                "icon", "🍔",
                "sortOrder", 0
        );
        JsonNode result = apiPost("/v1/categories", body);

        assertEquals(0, result.get("code").asInt(), "创建分类应成功: " + result);
        assertNotNull(result.get("data"));
        createdCategoryId = result.get("data").asLong();
        assertTrue(createdCategoryId > 0);
    }

    @Test
    @Order(4)
    void testSearchCategories_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/categories/search?keyword=餐");

        assertEquals(0, result.get("code").asInt(), "搜索分类应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(5)
    void testGetRecentCategories_shouldSucceed() {
        JsonNode result = executeGet(BASE_URL + "/v1/categories/recent?type=expense&limit=5");

        assertEquals(0, result.get("code").asInt(), "获取最近分类应成功: " + result);
        assertTrue(result.get("data").isArray());
    }

    @Test
    @Order(6)
    void testUpdateCategory_shouldSucceed() {
        assertNotNull(createdCategoryId, "需要先创建分类");

        Map<String, Object> body = map(
                "name", "更新后餐饮" + UNIQUE_SUFFIX,
                "type", "expense",
                "icon", "🍕"
        );
        JsonNode result = apiPut("/v1/categories/" + createdCategoryId, body);

        assertEquals(0, result.get("code").asInt(), "更新分类应成功: " + result);
    }

    @Test
    @Order(7)
    void testGetCategoryUsageCount_shouldSucceed() {
        assertNotNull(createdCategoryId, "需要先创建分类");

        JsonNode result = executeGet(BASE_URL + "/v1/categories/" + createdCategoryId + "/usage");

        assertEquals(0, result.get("code").asInt(), "获取使用次数应成功: " + result);
        assertNotNull(result.get("data").get("count"));
    }

    @Test
    @Order(8)
    void testGetMigrationOptions_shouldSucceed() {
        assertNotNull(createdCategoryId, "需要先创建分类");

        JsonNode result = executeGet(BASE_URL + "/v1/categories/" + createdCategoryId + "/migration-options");

        assertEquals(0, result.get("code").asInt(), "获取迁移选项应成功: " + result);
    }

    @Test
    @Order(9)
    void testCreateCategory_withoutToken_shouldFail() {
        Map<String, Object> body = map(
                "name", "无权限分类",
                "type", "expense"
        );
        JsonNode result = executePost(BASE_URL + "/v1/categories", body, null);

        assertNotEquals(0, result.get("code").asInt(), "无Token创建分类应失败: " + result);
    }

    @Test
    @Order(10)
    void testDeleteCategory_shouldSucceed() {
        Map<String, Object> body = map(
                "name", "待删除分类" + UNIQUE_SUFFIX,
                "type", "expense",
                "icon", "🗑"
        );
        JsonNode createResult = apiPost("/v1/categories", body);
        assertEquals(0, createResult.get("code").asInt());
        Long deleteId = createResult.get("data").asLong();

        JsonNode result = apiDelete("/v1/categories/" + deleteId);

        assertEquals(0, result.get("code").asInt(), "删除分类应成功: " + result);
    }

    @Test
    @Order(11)
    void testDeleteCategory_nonExistent_shouldFail() {
        JsonNode result = apiDelete("/v1/categories/999999");

        assertNotEquals(0, result.get("code").asInt(), "删除不存在的分类应失败: " + result);
    }
}
