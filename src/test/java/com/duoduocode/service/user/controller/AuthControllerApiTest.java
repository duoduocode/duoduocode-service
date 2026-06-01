package com.duoduocode.service.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("api")
class AuthControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;

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

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    @Test
    @Order(1)
    void testDevLogin_shouldReturnToken() {
        Map<String, Object> body = map("userId", 1L);
        JsonNode result = apiPost("/v1/auth/dev-login", body);

        assertEquals(0, result.get("code").asInt(), "dev登录应成功: " + result);
        assertNotNull(result.get("data"));
        assertNotNull(result.get("data").get("token"));
        assertTrue(result.get("data").get("token").asText().length() > 0);

        authToken = result.get("data").get("token").asText();
    }

    @Test
    @Order(2)
    void testDevLogin_missingUserId_shouldFail() {
        Map<String, Object> body = new LinkedHashMap<>();
        JsonNode result = apiPost("/v1/auth/dev-login", body);

        assertNotEquals(0, result.get("code").asInt(), "缺少userId应失败: " + result);
    }

    @Test
    @Order(3)
    void testDevLogin_newUser_shouldAutoCreate() {
        Map<String, Object> body = map("userId", 999999L);
        JsonNode result = apiPost("/v1/auth/dev-login", body);

        assertEquals(0, result.get("code").asInt(), "新用户dev登录应自动创建: " + result);
        assertNotNull(result.get("data").get("token"));
    }

    @Test
    @Order(4)
    void testLogout_shouldSucceed() {
        Map<String, Object> body = new LinkedHashMap<>();
        JsonNode result = apiPost("/v1/auth/logout", body);

        assertEquals(0, result.get("code").asInt(), "退出登录应成功: " + result);
    }

    @Test
    @Order(5)
    void testRefreshToken_shouldReturnNewToken() {
        assertNotNull(authToken, "需要先登录获取token");

        Map<String, Object> body = map("token", authToken);
        JsonNode result = apiPost("/v1/auth/refresh-token", body);

        assertEquals(0, result.get("code").asInt(), "刷新token应成功: " + result);
        assertNotNull(result.get("data").get("token"));
        assertNotEquals(authToken, result.get("data").get("token").asText(), "刷新后的token应不同于原token");
    }

    @Test
    @Order(6)
    void testRefreshToken_emptyToken_shouldFail() {
        Map<String, Object> body = map("token", "");
        JsonNode result = apiPost("/v1/auth/refresh-token", body);

        assertNotEquals(0, result.get("code").asInt(), "空token刷新应失败: " + result);
    }

    @Test
    @Order(7)
    void testRefreshToken_invalidToken_shouldFail() {
        Map<String, Object> body = map("token", "invalid_token_xyz");
        JsonNode result = apiPost("/v1/auth/refresh-token", body);

        assertNotEquals(0, result.get("code").asInt(), "无效token刷新应失败: " + result);
    }

    @Test
    @Order(8)
    void testLogin_emptyCode_shouldFail() {
        Map<String, Object> body = map("code", "");
        JsonNode result = apiPost("/v1/auth/login", body);

        assertNotEquals(0, result.get("code").asInt(), "空code登录应失败: " + result);
    }
}
