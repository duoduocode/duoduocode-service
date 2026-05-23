package com.duoduocode.service.user.service;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    private Long testUserId;

    private String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_user_service_" + System.currentTimeMillis());
        user.setNickname("测试用户");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setGender(1);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    @Test
    void getUserProfile_shouldReturnProfileWhenUserExists() {
        Map<String, Object> profile = userService.getUserProfile(testUserId);

        assertNotNull(profile);
        assertEquals(testUserId, profile.get("id"));
        assertEquals("测试用户", profile.get("nickname"));
        assertEquals("https://example.com/avatar.jpg", profile.get("avatarUrl"));
    }

    @Test
    void getUserProfile_shouldThrowExceptionWhenUserNotExists() {
        assertThrows(BusinessException.class, () -> {
            userService.getUserProfile(99999L);
        });
    }

    @Test
    void updateNickname_shouldUpdateSuccessfully() {
        userService.updateNickname(testUserId, "新昵称");

        User user = userMapper.selectById(testUserId);
        assertEquals("新昵称", user.getNickname());
    }

    @Test
    void updateNickname_shouldThrowExceptionWhenNicknameEmpty() {
        assertThrows(BusinessException.class, () -> {
            userService.updateNickname(testUserId, "");
        });
    }

    @Test
    void updateNickname_shouldThrowExceptionWhenNicknameTooLong() {
        String longNickname = repeatString("a", 31);
        assertThrows(BusinessException.class, () -> {
            userService.updateNickname(testUserId, longNickname);
        });
    }

    @Test
    void updateAvatarUrl_shouldUpdateSuccessfully() {
        userService.updateAvatarUrl(testUserId, "https://example.com/new_avatar.jpg");

        User user = userMapper.selectById(testUserId);
        assertEquals("https://example.com/new_avatar.jpg", user.getAvatarUrl());
    }

    @Test
    void updateAvatarUrl_shouldThrowExceptionWhenUrlEmpty() {
        assertThrows(BusinessException.class, () -> {
            userService.updateAvatarUrl(testUserId, "");
        });
    }

    @Test
    void updateProfile_shouldUpdateMultipleFields() {
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", "批量更新昵称");
        data.put("avatarUrl", "https://example.com/batch_avatar.jpg");
        data.put("gender", 0);

        userService.updateProfile(testUserId, data);

        User user = userMapper.selectById(testUserId);
        assertEquals("批量更新昵称", user.getNickname());
        assertEquals("https://example.com/batch_avatar.jpg", user.getAvatarUrl());
        assertEquals(0, user.getGender());
    }

    @Test
    void updateProfile_shouldIgnoreInvalidNickname() {
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", repeatString("a", 31));

        userService.updateProfile(testUserId, data);

        User user = userMapper.selectById(testUserId);
        assertEquals("测试用户", user.getNickname());
    }

    @Test
    void updateProfile_shouldThrowExceptionWhenUserNotExists() {
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", "新昵称");

        assertThrows(BusinessException.class, () -> {
            userService.updateProfile(99999L, data);
        });
    }

    @Test
    void login_shouldThrowExceptionWhenCodeEmpty() {
        assertThrows(BusinessException.class, () -> {
            userService.login("");
        });
    }

    @Test
    void login_shouldThrowExceptionWhenCodeNull() {
        assertThrows(BusinessException.class, () -> {
            userService.login(null);
        });
    }
}