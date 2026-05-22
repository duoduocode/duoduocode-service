package com.duoduocode.service.user.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import com.duoduocode.service.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 * 处理用户资料的获取和更新
 */
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户资料
     * GET /v1/user/profile
     *
     * @return 用户资料（昵称、头像、手机号等）
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        Long userId = SecurityContext.requireUserId();
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
    public Result<Void> updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = SecurityContext.requireUserId();
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
    public Result<Void> updateNickname(@RequestBody Map<String, String> body) {
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
    public Result<Void> updateAvatar(@RequestBody Map<String, String> body) {
        Long userId = SecurityContext.requireUserId();
        String avatarUrl = body.get("avatarUrl");
        userService.updateAvatarUrl(userId, avatarUrl);
        return Result.success("头像更新成功", null);
    }
}
