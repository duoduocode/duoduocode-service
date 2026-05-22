package com.duoduocode.service.recurring.controller;

import com.duoduocode.service.common.Result;
import com.duoduocode.service.recurring.dto.RecurringTemplateDTO;
import com.duoduocode.service.recurring.dto.RecurringTemplateVO;
import com.duoduocode.service.recurring.service.RecurringTemplateService;
import com.duoduocode.service.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 周期交易模板控制器
 * 提供周期交易模板管理相关API
 */
@RestController
@RequestMapping("/v1/recurring-templates")
@RequiredArgsConstructor
public class RecurringTemplateController {

    private final RecurringTemplateService recurringTemplateService;

    /**
     * 获取周期模板列表
     * GET /v1/recurring-templates
     *
     * @return 模板列表
     */
    @GetMapping
    public Result<List<RecurringTemplateVO>> getTemplateList() {
        Long userId = SecurityContext.requireUserId();
        List<RecurringTemplateVO> templates = recurringTemplateService.getTemplateList(userId);
        return Result.success(templates);
    }

    /**
     * 获取模板详情
     * GET /v1/recurring-templates/{id}
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public Result<RecurringTemplateVO> getTemplateDetail(@PathVariable Long id) {
        RecurringTemplateVO template = recurringTemplateService.getTemplateDetail(id);
        return Result.success(template);
    }

    /**
     * 创建模板
     * POST /v1/recurring-templates
     *
     * @param dto 模板数据
     * @return 创建的模板ID
     */
    @PostMapping
    public Result<Long> createTemplate(@RequestBody RecurringTemplateDTO dto) {
        Long userId = SecurityContext.requireUserId();
        Long templateId = recurringTemplateService.createTemplate(userId, dto);
        return Result.success("创建成功", templateId);
    }

    /**
     * 更新模板
     * PUT /v1/recurring-templates/{id}
     *
     * @param id  模板ID
     * @param dto 模板数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody RecurringTemplateDTO dto) {
        recurringTemplateService.updateTemplate(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除模板
     * DELETE /v1/recurring-templates/{id}
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        recurringTemplateService.deleteTemplate(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取到期提醒列表
     * GET /v1/recurring-templates/due
     *
     * @return 到期模板列表
     */
    @GetMapping("/due")
    public Result<List<RecurringTemplateVO>> getDueTemplates() {
        Long userId = SecurityContext.requireUserId();
        List<RecurringTemplateVO> templates = recurringTemplateService.getDueTemplates(userId);
        return Result.success(templates);
    }

    /**
     * 手动触发提醒
     * POST /v1/recurring-templates/{id}/trigger
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/trigger")
    public Result<Void> triggerTemplate(@PathVariable Long id) {
        recurringTemplateService.triggerTemplate(id);
        return Result.success("触发成功", null);
    }

    /**
     * 暂停模板
     * POST /v1/recurring-templates/{id}/pause
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/pause")
    public Result<Void> pauseTemplate(@PathVariable Long id) {
        recurringTemplateService.pauseTemplate(id);
        return Result.success("暂停成功", null);
    }

    /**
     * 恢复模板
     * POST /v1/recurring-templates/{id}/resume
     *
     * @param id 模板ID
     * @return 操作结果
     */
    @PostMapping("/{id}/resume")
    public Result<Void> resumeTemplate(@PathVariable Long id) {
        recurringTemplateService.resumeTemplate(id);
        return Result.success("恢复成功", null);
    }
}
