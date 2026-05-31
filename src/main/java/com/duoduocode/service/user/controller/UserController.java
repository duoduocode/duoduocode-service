package com.duoduocode.service.user.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 * 处理用户资料的获取和更新
 */
@Slf4j
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户资料的获取和更新")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户资料
     * GET /v1/user/profile
     *
     * @return 用户资料（昵称、头像、手机号等）
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户资料", description = "获取当前登录用户的个人资料")
    public Result<Map<String, Object>> getProfile() {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/user/profile - userId={}", userId);
        Map<String, Object> profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * 更新用户资料
     * PUT /v1/user/profile
     *
     * 支持批量更新昵称、头像、性别等
     *
     * @param body 更新数据 {"nickname": "xxx", "avatarUrl": "xxx", "gender": 1}
     * @return 操作结果
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户资料", description = "批量更新用户资料（昵称、头像、性别等）")
    public Result<Void> updateProfile(@Parameter(description = "更新数据") @RequestBody Map<String, Object> body) {
        Long userId = SecurityContext.requireUserId();
        log.info("PUT /v1/user/profile - userId={}, fields={}", userId, body.keySet());
        userService.updateProfile(userId, body);
        return Result.success("更新成功", null);
    }

    /**
     * 单独更新昵称
     * PUT /v1/user/nickname
     *
     * @param body {"nickname": "xxx"}
     * @return 操作结果
     */
    @PutMapping("/nickname")
    @Operation(summary = "更新昵称", description = "单独更新用户昵称")
    public Result<Void> updateNickname(@Parameter(description = "昵称数据") @RequestBody Map<String, String> body) {
        Long userId = SecurityContext.requireUserId();
        String nickname = body.get("nickname");
        userService.updateNickname(userId, nickname);
        return Result.success("昵称更新成功", null);
    }

    /**
     * 单独更新头像
     * PUT /v1/user/avatar
     *
     * @param body {"avatarUrl": "xxx"}
     * @return 操作结果
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像", description = "单独更新用户头像")
    public Result<Void> updateAvatar(@Parameter(description = "头像数据") @RequestBody Map<String, String> body) {
        Long userId = SecurityContext.requireUserId();
        String avatarUrl = body.get("avatarUrl");
        userService.updateAvatarUrl(userId, avatarUrl);
        return Result.success("头像更新成功", null);
    }

    /**
     * 获取用户统计信息
     * GET /v1/user/stats
     *
     * @return {totalDays, totalCount, netAsset}
     */
    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取用户记账天数、总笔数、净资产")
    public Result<Map<String, Object>> getStats() {
        Long userId = SecurityContext.requireUserId();
        log.debug("GET /v1/user/stats - userId={}", userId);
        Map<String, Object> stats = userService.getStats(userId);
        return Result.success(stats);
    }
}
