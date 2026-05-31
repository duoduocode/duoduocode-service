package com.duoduocode.service.user.service;

import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务类
 * 处理用户登录、注册、资料更新等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Value("${wechat.mini.appid}")
    private String appid;

    @Value("${wechat.mini.secret}")
    private String secret;

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * 微信登录
     * 1. 用 code 换取 openid
     * 2. 根据 openid 查找/创建用户
     * 3. 返回用户信息和是否新用户
     *
     * @param code 微信 wx.login() 获取的 code
     * @return 登录结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "登录code不能为空");
        }

        // 1. code 换 openid + session_key
        String openid = code2Session(code);
        log.info("获取到微信openid={}", openid);
        if (openid == null || openid.isEmpty()) {
            throw new BusinessException(ResultCode.LOGIN_ERROR, "微信登录失败，无法获取用户标识");
        }

        // 2. 根据 openid 查找用户
        User user = userMapper.selectByOpenid(openid);

        boolean isNewUser = false;
        if (user == null) {
            // 3. 新用户，自动创建
            user = new User();
            user.setOpenid(openid);
            user.setNickname("");
            user.setAvatarUrl("");
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setIsDeleted(false);
            userMapper.insert(user);
            isNewUser = true;
            log.info("新用户注册成功, userId={}, openid={}", user.getId(), openid);
        }

        // 4. 更新最后登录时间
        User updateLogin = new User();
        updateLogin.setId(user.getId());
        updateLogin.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(updateLogin);

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("openid", openid);
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("isNewUser", isNewUser);

        return result;
    }

    /**
     * 获取用户资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    public Map<String, Object> getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("nickname", user.getNickname());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("gender", user.getGender());
        profile.put("createdAt", user.getCreatedAt());

        return profile;
    }

    /**
     * 更新用户昵称
     *
     * @param userId   用户ID
     * @param nickname 新昵称
     */
    public void updateNickname(Long userId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "昵称不能为空");
        }
        if (nickname.length() > 30) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "昵称不能超过30个字符");
        }
        userMapper.updateNickname(userId, nickname.trim());
    }

    /**
     * 更新用户头像
     *
     * @param userId    用户ID
     * @param avatarUrl 头像URL
     */
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "头像地址不能为空");
        }
        userMapper.updateAvatarUrl(userId, avatarUrl.trim());
    }

    /**
     * 更新用户资料（批量更新）
     *
     * @param userId 用户ID
     * @param data   更新数据
     */
    public void updateProfile(Long userId, Map<String, Object> data) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }

        User updateUser = new User();
        updateUser.setId(userId);

        if (data.containsKey("nickname")) {
            String nickname = (String) data.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty() && nickname.length() <= 30) {
                updateUser.setNickname(nickname.trim());
            }
        }
        if (data.containsKey("avatarUrl")) {
            String avatarUrl = (String) data.get("avatarUrl");
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                updateUser.setAvatarUrl(avatarUrl.trim());
            }
        }
        if (data.containsKey("gender")) {
            updateUser.setGender((Integer) data.get("gender"));
        }
        if (data.containsKey("phone")) {
            updateUser.setPhone((String) data.get("phone"));
        }
        if (data.containsKey("email")) {
            updateUser.setEmail((String) data.get("email"));
        }

        updateUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(updateUser);
    }

    /**
     * 获取用户统计信息：记账天数、总笔数、净资产
     *
     * @param userId 用户ID
     * @return {totalDays, totalCount, netAsset}
     */
    public Map<String, Object> getStats(Long userId) {
        Map<String, Object> stats = userMapper.selectStats(userId);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("totalDays", 0);
            stats.put("totalCount", 0);
            stats.put("netAsset", BigDecimal.ZERO);
        }
        return stats;
    }

    /**
     * 调用微信 code2Session 接口，用 code 换取 openid
     *
     * @param code 微信登录 code
     * @return openid，失败返回 null
     */
    private String code2Session(String code) {
        try {
            String url = String.format(WX_LOGIN_URL, appid, secret, code);
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            JsonNode jsonNode = objectMapper.readTree(response);
            String openid = jsonNode.has("openid") ? jsonNode.get("openid").asText() : null;
            String errMsg = jsonNode.has("errmsg") ? jsonNode.get("errmsg").asText() : null;

            if (openid == null && errMsg != null) {
                log.error("微信code2Session失败: {}", response);
            }

            return openid;
        } catch (Exception e) {
            log.error("调用微信code2Session异常", e);
            return null;
        }
    }
}
