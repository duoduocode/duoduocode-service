package com.duoduocode.service.user.mapper;

import com.duoduocode.service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class UserMapperIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    private User createTestUser() {
        User user = new User();
        user.setOpenid("test_openid_" + System.currentTimeMillis());
        user.setNickname("测试用户");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setGender(1);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    @Test
    void selectByOpenid_shouldReturnUserWhenExists() {
        User user = createTestUser();

        User result = userMapper.selectByOpenid(user.getOpenid());

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getOpenid(), result.getOpenid());
        assertEquals("测试用户", result.getNickname());
    }

    @Test
    void selectByOpenid_shouldReturnNullWhenNotExists() {
        User result = userMapper.selectByOpenid("non_existent_openid_" + System.currentTimeMillis());

        assertNull(result);
    }

    @Test
    void selectById_shouldReturnUserWhenExists() {
        User user = createTestUser();

        User result = userMapper.selectById(user.getId());

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getOpenid(), result.getOpenid());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExists() {
        User result = userMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void insert_shouldCreateUser() {
        User user = new User();
        user.setOpenid("new_openid_" + System.currentTimeMillis());
        user.setNickname("新用户");
        user.setAvatarUrl("");
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.insert(user);

        assertEquals(1, result);
        assertNotNull(user.getId());

        User saved = userMapper.selectById(user.getId());
        assertNotNull(saved);
        assertEquals("新用户", saved.getNickname());
    }

    @Test
    void updateById_shouldUpdateUser() {
        User user = createTestUser();

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname("更新后的昵称");
        updateUser.setAvatarUrl("https://example.com/new_avatar.jpg");

        int result = userMapper.updateById(updateUser);

        assertEquals(1, result);

        User updated = userMapper.selectById(user.getId());
        assertEquals("更新后的昵称", updated.getNickname());
        assertEquals("https://example.com/new_avatar.jpg", updated.getAvatarUrl());
    }

    @Test
    void updateAvatarUrl_shouldUpdateAvatar() {
        User user = createTestUser();

        int result = userMapper.updateAvatarUrl(user.getId(), "https://example.com/updated_avatar.png");

        assertEquals(1, result);

        User updated = userMapper.selectById(user.getId());
        assertEquals("https://example.com/updated_avatar.png", updated.getAvatarUrl());
    }

    @Test
    void updateNickname_shouldUpdateNickname() {
        User user = createTestUser();

        int result = userMapper.updateNickname(user.getId(), "新昵称测试");

        assertEquals(1, result);

        User updated = userMapper.selectById(user.getId());
        assertEquals("新昵称测试", updated.getNickname());
    }

    @Test
    void updateAvatarUrl_shouldReturnZeroWhenUserNotExists() {
        int result = userMapper.updateAvatarUrl(99999L, "https://example.com/avatar.png");

        assertEquals(0, result);
    }

    @Test
    void updateNickname_shouldReturnZeroWhenUserNotExists() {
        int result = userMapper.updateNickname(99999L, "新昵称");

        assertEquals(0, result);
    }
}