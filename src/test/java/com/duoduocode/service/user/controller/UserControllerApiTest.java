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
class UserControllerApiTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RestTemplate restTemplate = new RestTemplate();

    private static String authToken;
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
    void testGetProfile_shouldReturnUserData() {
        JsonNode result = executeGet(BASE_URL + "/v1/user/profile");

        assertEquals(0, result.get("code").asInt(), "获取用户资料应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(2)
    void testUpdateProfile_shouldSucceed() {
        Map<String, Object> body = map(
                "nickname", "测试用户" + UNIQUE_SUFFIX,
                "gender", 1
        );
        JsonNode result = apiPut("/v1/user/profile", body);

        assertEquals(0, result.get("code").asInt(), "更新用户资料应成功: " + result);

        JsonNode profile = executeGet(BASE_URL + "/v1/user/profile");
        assertEquals(0, profile.get("code").asInt());
    }

    @Test
    @Order(3)
    void testUpdateNickname_shouldSucceed() {
        Map<String, Object> body = map("nickname", "新昵称" + UNIQUE_SUFFIX);
        JsonNode result = apiPut("/v1/user/nickname", body);

        assertEquals(0, result.get("code").asInt(), "更新昵称应成功: " + result);
    }

    @Test
    @Order(4)
    void testUpdateNickname_empty_shouldFail() {
        Map<String, Object> body = map("nickname", "");
        JsonNode result = apiPut("/v1/user/nickname", body);

        assertNotEquals(0, result.get("code").asInt(), "空昵称应失败: " + result);
    }

    @Test
    @Order(5)
    void testUpdateAvatar_shouldSucceed() {
        Map<String, Object> body = map("avatarUrl", "https://example.com/avatar_" + UNIQUE_SUFFIX + ".png");
        JsonNode result = apiPut("/v1/user/avatar", body);

        assertEquals(0, result.get("code").asInt(), "更新头像应成功: " + result);
    }

    @Test
    @Order(6)
    void testGetStats_shouldReturnStats() {
        JsonNode result = executeGet(BASE_URL + "/v1/user/stats");

        assertEquals(0, result.get("code").asInt(), "获取统计信息应成功: " + result);
        assertNotNull(result.get("data"));
    }

    @Test
    @Order(7)
    void testGetProfile_withoutToken_shouldFail() {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/v1/user/profile", HttpMethod.GET, entity, String.class);
            JsonNode result = objectMapper.readTree(response.getBody());
            assertNotEquals(0, result.get("code").asInt(), "无Token获取资料应失败: " + result);
        } catch (Exception e) {
            assertNotNull(e, "无Token请求应被拒绝");
        }
    }
}
