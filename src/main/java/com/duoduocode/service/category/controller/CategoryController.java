package com.duoduocode.service.category.controller;

import com.duoduocode.service.category.dto.CategoryDTO;
import com.duoduocode.service.category.dto.CategoryMigrateDTO;
import com.duoduocode.service.category.dto.CategoryTreeVO;
import com.duoduocode.service.category.dto.CategoryVO;
import com.duoduocode.service.category.service.CategoryService;
import com.duoduocode.service.common.Result;
import com.duoduocode.service.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类控制器
 * 提供分类管理相关API
 */
@Slf4j
@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "分类的增删改查及迁移")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     * GET /v1/categories
     *
     * @param type 分类类型（expense/income），为空则返回全部
     * @return 树形结构的分类列表
     */
    @GetMapping
    @Operation(summary = "获取分类列表", description = "获取当前用户的分类列表，支持按类型筛选")
    public Result<List<CategoryTreeVO>> getCategoryList(
            @Parameter(description = "分类类型（expense/income）") @RequestParam(required = false) String type) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryTreeVO> categories = categoryService.getCategoryList(userId, type);
        return Result.success(categories);
    }

    /**
     * 获取最近使用分类
     * GET /v1/categories/recent
     *
     * @param type  分类类型（expense/income）
     * @param limit 返回数量，默认10
     * @return 最近使用的分类列表
     */
    @GetMapping("/recent")
    @Operation(summary = "获取最近使用分类", description = "获取用户最近使用的分类列表")
    public Result<List<CategoryVO>> getRecentCategories(
            @Parameter(description = "分类类型（expense/income）") @RequestParam(required = false) String type,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryVO> categories = categoryService.getRecentCategories(userId, type, limit);
        return Result.success(categories);
    }

    /**
     * 搜索分类
     * GET /v1/categories/search
     *
     * @param keyword 搜索关键词
     * @return 匹配的分类列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索分类", description = "根据关键词搜索分类")
    public Result<List<CategoryVO>> searchCategories(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryVO> categories = categoryService.searchCategories(userId, keyword);
        return Result.success(categories);
    }

    /**
     * 获取快捷二级分类
     * GET /v1/categories/{parentId}/quick-children
     *
     * @param parentId 父分类ID
     * @return 快捷二级分类列表（最近3个）
     */
    @GetMapping("/{parentId}/quick-children")
    @Operation(summary = "获取快捷二级分类", description = "获取父分类下最近使用的3个二级分类")
    public Result<List<CategoryTreeVO>> getQuickChildren(
            @Parameter(description = "父分类ID") @PathVariable Long parentId) {
        List<CategoryTreeVO> children = categoryService.getQuickChildren(parentId);
        return Result.success(children);
    }

    /**
     * 创建分类
     * POST /v1/categories
     *
     * @param dto 分类数据
     * @return 创建的分类ID
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "创建新的分类")
    public Result<Long> createCategory(@Parameter(description = "分类数据") @RequestBody CategoryDTO dto) {
        Long userId = SecurityContext.requireUserId();
        log.info("POST /v1/categories - userId={}, name={}, type={}", userId, dto.getName(), dto.getType());
        Long categoryId = categoryService.createCategory(userId, dto);
        log.info("分类创建成功, categoryId={}", categoryId);
        return Result.success("创建成功", categoryId);
    }

    /**
     * 更新分类
     * PUT /v1/categories/{id}
     *
     * @param id  分类ID
     * @param dto 分类数据
     * @return 操作结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新分类信息")
    public Result<Void> updateCategory(@Parameter(description = "分类ID") @PathVariable Long id, @Parameter(description = "分类数据") @RequestBody CategoryDTO dto) {
        categoryService.updateCategory(id, dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除分类
     * DELETE /v1/categories/{id}
     *
     * @param id          分类ID
     * @param migrateToId 迁移目标分类ID（可选，仅一级分类需要）
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除分类，支持迁移关联交易到其他分类")
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "迁移目标分类ID") @RequestParam(required = false) Long migrateToId) {
        categoryService.deleteCategory(id, migrateToId);
        return Result.success("删除成功", null);
    }

    // ===== 分类迁移相关接口 =====

    /**
     * 迁移分类
     * POST /v1/categories/{id}/migrate
     *
     * @param id  分类ID
     * @param dto 迁移参数
     * @return 操作结果
     */
    @PostMapping("/{id}/migrate")
    @Operation(summary = "迁移分类", description = "将分类下的交易迁移到其他分类")
    public Result<Void> migrateCategory(@Parameter(description = "分类ID") @PathVariable Long id, @Parameter(description = "迁移参数") @RequestBody CategoryMigrateDTO dto) {
        categoryService.migrateCategory(id, dto);
        return Result.success("迁移成功", null);
    }

    /**
     * 获取分类使用次数
     * GET /v1/categories/{id}/usage
     *
     * @param id 分类ID
     * @return 使用次数
     */
    @GetMapping("/{id}/usage")
    @Operation(summary = "获取分类使用次数", description = "获取分类被交易使用的次数")
    public Result<Map<String, Object>> getCategoryUsageCount(@Parameter(description = "分类ID") @PathVariable Long id) {
        Long count = categoryService.getCategoryUsageCount(id);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 获取可迁移的目标分类选项
     * GET /v1/categories/{id}/migration-options
     *
     * @param id 分类ID
     * @return 可迁移的目标分类列表
     */
    @GetMapping("/{id}/migration-options")
    @Operation(summary = "获取迁移目标选项", description = "获取可迁移交易的目标分类列表")
    public Result<List<CategoryVO>> getMigrationOptions(@Parameter(description = "分类ID") @PathVariable Long id) {
        Long userId = SecurityContext.requireUserId();
        List<CategoryVO> options = categoryService.getMigrationOptions(userId, id);
        return Result.success(options);
    }
}
