package com.duoduocode.service.notification.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.notification.dto.NotificationListVO;
import com.duoduocode.service.notification.dto.NotificationReadDTO;
import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息/待办控制器
 * 消息列表、标记已读（仅通知型）、未读数
 */
@Slf4j
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "消息中心", description = "站内消息/待办的列表、已读与未读数")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 消息列表（待办置顶，组内创建时间倒序）
     * GET /v1/notifications?page=1&pageSize=20&msgType=all|todo|notice
     */
    @GetMapping
    @Operation(summary = "消息列表", description = "分页查询站内消息/待办，待办置顶，附带未读数统计")
    public Result<NotificationListVO> list(
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数，默认 20，上限 100") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "消息形态筛选 all/todo/notice") @RequestParam(required = false) String msgType) {
        Long userId = SecurityContext.requireUserId();
        return Result.success(notificationService.getList(userId, page, pageSize, msgType));
    }

    /**
     * 标记已读
     * POST /v1/notifications/read
     * body: { "ids": [1,2] } | { "all": true }
     * 仅通知型生效；待办以完成动作消化（产品设计 A6）
     */
    @PostMapping("/read")
    @Operation(summary = "标记已读", description = "按 ids 或 all=true 标记已读，仅通知型生效，待办以完成动作消化")
    public Result<Map<String, Object>> markRead(
            @Parameter(description = "已读请求：ids 数组或 all=true") @RequestBody NotificationReadDTO dto) {
        Long userId = SecurityContext.requireUserId();
        int updated = notificationService.markRead(userId, dto);
        Map<String, Object> data = new HashMap<>();
        data.put("updated", updated);
        return Result.success(data);
    }

    /**
     * 未读数
     * GET /v1/notifications/unread-count
     * → { "todo": 2, "notice": 3, "total": 5 }
     */
    @GetMapping("/unread-count")
    @Operation(summary = "未读数", description = "通知未读数 + 待办未消化数：{todo, notice, total}")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = SecurityContext.requireUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }
}
