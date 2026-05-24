package com.duoduocode.service.user.controller;

import com.duoduocode.service.category.init.CategoryDataInitializer;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.JwtUtils;
import com.duoduocode.service.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、Token刷新、退出等
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、Token刷新、退出等认证操作")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final CategoryDataInitializer categoryDataInitializer;

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
    @Operation(summary = "微信登录", description = "通过微信code换取openid并完成登录，自动创建新用户（如不存在）")
    public Result<Map<String, Object>> login(@Parameter(description = "登录请求") @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            log.warn("登录失败: code为空");
            return Result.fail("登录code不能为空");
        }

        log.info("微信登录请求, code={}", code.substring(0, Math.min(code.length(), 8)) + "...");

        Map<String, Object> loginResult = userService.login(code);

        Long userId = (Long) loginResult.get("userId");
        String openid = (String) loginResult.get("openid");

        String token = jwtUtils.generateToken(userId, openid);

        log.info("登录成功, userId={}, isNewUser={}", userId, loginResult.get("isNewUser"));

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
    @Operation(summary = "刷新Token", description = "使用旧Token换取新Token，延长登录有效期")
    public Result<Map<String, Object>> refreshToken(@Parameter(description = "Token请求") @RequestBody Map<String, String> body) {
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
    @Operation(summary = "开发环境登录", description = "跳过微信授权，直接用userId签发JWT Token（仅开发环境可用）")
    public Result<Map<String, Object>> devLogin(@Parameter(description = "登录请求") @RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        if (userIdObj == null) {
            return Result.fail("userId不能为空");
        }

        Long userId = ((Number) userIdObj).longValue();

        String token = jwtUtils.generateToken(userId);

        categoryDataInitializer.initForUser(userId);

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
    @Operation(summary = "退出登录", description = "前端清除Token即可，后端无状态无需额外处理")
    public Result<Void> logout() {
        // JWT 是无状态的，退出登录由前端清除 token
        // 如果需要支持 Token 黑名单，可以在这里加入 Redis 缓存
        return Result.success("退出成功", null);
    }
}
