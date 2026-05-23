package com.duoduocode.service.category.service;

import com.duoduocode.service.category.dto.CategoryDTO;
import com.duoduocode.service.category.dto.CategoryMigrateDTO;
import com.duoduocode.service.category.dto.CategoryTreeVO;
import com.duoduocode.service.category.dto.CategoryVO;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import com.duoduocode.service.transaction.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;

    /**
     * 获取分类列表（两级树形结构）
     *
     * @param userId 用户ID
     * @param type   分类类型（expense/income），为空则返回全部
     * @return 树形结构的分类列表
     */
    public List<CategoryTreeVO> getCategoryList(Long userId, String type) {
        // 查询所有分类
        List<Category> categories = categoryMapper.selectByUserIdAndType(userId, type);

        // 分离一级分类和二级分类
        List<Category> parentCategories = categories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        Map<Long, List<Category>> childrenMap = new HashMap<>();
        for (Category category : categories) {
            if (category.getParentId() != null) {
                childrenMap.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category);
            }
        }

        // 构建树形结构
        List<CategoryTreeVO> result = new ArrayList<>();
        for (Category parent : parentCategories) {
            CategoryTreeVO treeVO = convertToTreeVO(parent);

            // 添加子分类
            List<Category> children = childrenMap.get(parent.getId());
            if (children != null && !children.isEmpty()) {
                for (Category child : children) {
                    treeVO.getChildren().add(convertToTreeVO(child));
                }
            }

            result.add(treeVO);
        }

        return result;
    }

    /**
     * 获取最近使用的分类
     *
     * @param userId 用户ID
     * @param type   分类类型
     * @param limit  返回数量
     * @return 最近使用的分类列表
     */
    public List<CategoryVO> getRecentCategories(Long userId, String type, int limit) {
        List<Category> categories = categoryMapper.selectRecentUsed(userId, type, limit);
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 搜索分类（支持拼音）
     *
     * @param userId  用户ID
     * @param keyword 搜索关键词
     * @return 匹配的分类列表
     */
    public List<CategoryVO> searchCategories(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Category> categories = categoryMapper.searchByKeyword(userId, keyword.trim());
        return categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取快捷二级分类（最近3个）
     *
     * @param parentId 父分类ID
     * @return 快捷二级分类列表
     */
    public List<CategoryTreeVO> getQuickChildren(Long parentId) {
        // 验证父分类存在
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "父分类不存在");
        }

        List<Category> children = categoryMapper.selectQuickChildren(parentId, 3);
        return children.stream()
                .map(this::convertToTreeVO)
                .collect(Collectors.toList());
    }

    /**
     * 创建分类
     *
     * @param userId 用户ID
     * @param dto    分类数据传输对象
     * @return 创建的分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(Long userId, CategoryDTO dto) {
        // 验证必填参数
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名称不能为空");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类类型不能为空");
        }

        // 验证分类类型
        if (!isValidType(dto.getType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类类型无效，必须是 expense 或 income");
        }

        // 如果有父分类，验证父分类存在且类型一致
        if (dto.getParentId() != null) {
            Category parent = categoryMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "父分类不存在");
            }
            if (!dto.getType().equals(parent.getType())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "子分类类型必须与父分类一致");
            }
        }

        // 检查分类名称是否重复
        int count = categoryMapper.countByUserIdAndName(userId, dto.getName().trim(),
                dto.getType(), dto.getParentId(), null);
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "分类名称已存在");
        }

        // 创建分类实体
        Category category = new Category();
        category.setUserId(userId);
        category.setName(dto.getName().trim());
        category.setType(dto.getType());
        category.setParentId(dto.getParentId());
        category.setIcon(dto.getIcon() != null ? dto.getIcon() : (dto.getParentId() == null ? "📁" : "📄"));
        category.setColor(dto.getColor() != null ? dto.getColor() : "#07C160");
        category.setMonthlyBudget(dto.getMonthlyBudget());
        category.setWeeklyBudget(dto.getWeeklyBudget());
        category.setAlertThreshold(dto.getAlertThreshold());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());

        categoryMapper.insert(category);
        return category.getId();
    }

    /**
     * 更新分类
     *
     * @param id  分类ID
     * @param dto 分类数据传输对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryDTO dto) {
        // 检查分类是否存在
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        // 如果修改了名称，检查是否重复
        if (dto.getName() != null && !dto.getName().trim().equals(existing.getName())) {
            int count = categoryMapper.countByUserIdAndName(existing.getUserId(), dto.getName().trim(),
                    existing.getType(), existing.getParentId(), id);
            if (count > 0) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "分类名称已存在");
            }
        }

        // 更新分类
        Category category = new Category();
        category.setId(id);
        if (dto.getName() != null) category.setName(dto.getName().trim());
        if (dto.getIcon() != null) category.setIcon(dto.getIcon());
        if (dto.getColor() != null) category.setColor(dto.getColor());
        if (dto.getMonthlyBudget() != null) category.setMonthlyBudget(dto.getMonthlyBudget());
        if (dto.getWeeklyBudget() != null) category.setWeeklyBudget(dto.getWeeklyBudget());
        if (dto.getAlertThreshold() != null) category.setAlertThreshold(dto.getAlertThreshold());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());

        categoryMapper.updateById(category);
    }

    /**
     * 删除分类（软删除，需处理迁移）
     *
     * @param id          分类ID
     * @param migrateToId 迁移目标分类ID（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id, Long migrateToId) {
        // 检查分类是否存在
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        // 如果是一级分类，处理子分类
        if (category.getParentId() == null) {
            if (migrateToId != null) {
                // 验证迁移目标分类存在
                Category target = categoryMapper.selectById(migrateToId);
                if (target == null) {
                    throw new BusinessException(ResultCode.DATA_NOT_FOUND, "迁移目标分类不存在");
                }
                if (!target.getType().equals(category.getType())) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "迁移目标分类类型必须一致");
                }
                // 将子分类迁移到新的一级分类下
                categoryMapper.updateChildrenParentId(id, migrateToId);
            } else {
                // 如果没有指定迁移目标，同时删除所有子分类
                List<Category> children = categoryMapper.selectByParentId(id);
                for (Category child : children) {
                    categoryMapper.softDeleteById(child.getId());
                }
            }
        }

        // 软删除分类
        categoryMapper.softDeleteById(id);
    }

    // ===== 分类迁移相关方法 =====

    /**
     * 获取分类使用次数
     *
     * @param categoryId 分类ID
     * @return 使用次数
     */
    public Long getCategoryUsageCount(Long categoryId) {
        return transactionMapper.countByCategoryId(categoryId);
    }

    /**
     * 获取可迁移的目标分类选项
     *
     * @param userId     用户ID
     * @param categoryId 原分类ID
     * @return 可迁移的目标分类列表
     */
    public List<CategoryVO> getMigrationOptions(Long userId, Long categoryId) {
        // 查询原分类信息
        Category sourceCategory = categoryMapper.selectById(categoryId);
        if (sourceCategory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }

        // 查询同类型的其他分类（排除自身）
        List<Category> allCategories = categoryMapper.selectByUserIdAndType(userId, sourceCategory.getType());
        return allCategories.stream()
                .filter(c -> !c.getId().equals(categoryId))
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 迁移分类
     *
     * @param categoryId 原分类ID
     * @param dto        迁移参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateCategory(Long categoryId, CategoryMigrateDTO dto) {
        // 1. 验证原分类存在
        Category sourceCategory = categoryMapper.selectById(categoryId);
        if (sourceCategory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "原分类不存在");
        }

        // 2. 验证目标分类存在
        Long targetCategoryId = dto.getTargetCategoryId();
        if (targetCategoryId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "目标分类ID不能为空");
        }

        Category targetCategory = categoryMapper.selectById(targetCategoryId);
        if (targetCategory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "目标分类不存在");
        }

        // 3. 验证类型一致
        if (!sourceCategory.getType().equals(targetCategory.getType())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "源分类和目标分类类型必须一致");
        }

        // 4. 将原分类的交易迁移到目标分类
        transactionMapper.updateCategoryId(categoryId, targetCategoryId);

        // 5. 处理子分类
        Boolean mergeChildren = dto.getMergeChildren();
        if (mergeChildren != null && mergeChildren) {
            // 查询原分类的子分类
            List<Category> children = categoryMapper.selectByParentId(categoryId);
            for (Category child : children) {
                // 检查目标分类下是否有同名子分类
                List<Category> targetChildren = categoryMapper.selectByParentId(targetCategoryId);
                boolean hasSameNameChild = targetChildren.stream()
                        .anyMatch(tc -> tc.getName().equals(child.getName()));

                if (hasSameNameChild) {
                    // 同名子分类存在，将交易迁移到同名子分类，然后删除原子分类
                    Category sameNameChild = targetChildren.stream()
                            .filter(tc -> tc.getName().equals(child.getName()))
                            .findFirst()
                            .orElse(null);
                    if (sameNameChild != null) {
                        transactionMapper.updateCategoryId(child.getId(), sameNameChild.getId());
                        categoryMapper.softDeleteById(child.getId());
                    }
                } else {
                    // 同名子分类不存在，将子分类迁移到目标分类下
                    Category updatedChild = new Category();
                    updatedChild.setId(child.getId());
                    updatedChild.setParentId(targetCategoryId);
                    categoryMapper.updateById(updatedChild);
                }
            }
        }

        // 6. 软删除原分类
        categoryMapper.softDeleteById(categoryId);
    }

    // ===== 私有辅助方法 =====

    /**
     * 将实体转换为VO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    /**
     * 将实体转换为树形VO
     */
    private CategoryTreeVO convertToTreeVO(Category category) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setColor(category.getColor());
        return vo;
    }

    /**
     * 验证分类类型是否有效
     */
    private boolean isValidType(String type) {
        return "expense".equals(type) || "income".equals(type);
    }
}
