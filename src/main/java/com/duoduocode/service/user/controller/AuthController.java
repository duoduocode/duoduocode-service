package com.duoduocode.service.user.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.JwtUtils;
import com.duoduocode.service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、Token刷新、退出等
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * 微信登录
     * POST /v1/auth/login
     *
     * 前端调用 wx.login() 获取 code，传入后端换取 openid
     * 后端自动创建新用户（如不存在），签发 JWT Token
     *
     * @param body 请求体 {"code": "xxx"}
     * @return token + 用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return Result.fail("登录code不能为空");
        }

        // 调用微信登录，获取/创建用户
        Map<String, Object> loginResult = userService.login(code);

        Long userId = (Long) loginResult.get("userId");
        String openid = (String) loginResult.get("openid");

        // 签发 JWT Token
        String token = jwtUtils.generateToken(userId, openid);

        // 构建返回
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        data.put("nickname", loginResult.get("nickname"));
        data.put("avatarUrl", loginResult.get("avatarUrl"));
        data.put("isNewUser", loginResult.get("isNewUser"));

        return Result.success("登录成功", data);
    }

    /**
     * 刷新 Token
     * POST /v1/auth/refresh-token
     *
     * @param body 请求体 {"token": "xxx"}
     * @return 新的 token
     */
    @PostMapping("/refresh-token")
    public Result<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
        String oldToken = body.get("token");
        if (oldToken == null || oldToken.trim().isEmpty()) {
            return Result.fail("token不能为空");
        }

        // 从旧 token 解析用户ID
        Long userId = jwtUtils.getUserIdFromToken(oldToken);
        if (userId == null) {
            return Result.fail("无效的token");
        }

        // 签发新 token
        String newToken = jwtUtils.generateToken(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        return Result.success("刷新成功", data);
    }

    /**
     * [DEV] 开发环境直接登录
     * POST /v1/auth/dev-login
     *
     * 跳过微信授权，直接用 userId 签发 JWT Token，仅 dev profile 可用
     *
     * @param body 请求体 {"userId": 1}
     * @return token + 用户信息
     */
    @PostMapping("/dev-login")
    public Result<Map<String, Object>> devLogin(@RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        if (userIdObj == null) {
            return Result.fail("userId不能为空");
        }

        Long userId = ((Number) userIdObj).longValue();

        String token = jwtUtils.generateToken(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);

        return Result.success("dev登录成功", data);
    }

    /**
     * 退出登录
     * POST /v1/auth/logout
     *
     * 前端清除本地存储的 token 即可，后端无状态无需处理
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 是无状态的，退出登录由前端清除 token
        // 如果需要支持 Token 黑名单，可以在这里加入 Redis 缓存
        return Result.success("退出成功", null);
    }
}
