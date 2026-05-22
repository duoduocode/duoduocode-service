package com.duoduocode.service.category.mapper;

import com.duoduocode.service.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类 Mapper 接口
 */
@Mapper
public interface CategoryMapper {

    /**
     * 插入分类
     */
    int insert(Category category);

    /**
     * 根据ID更新分类
     */
    int updateById(Category category);

    /**
     * 根据ID查询分类
     */
    Category selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询分类列表
     */
    List<Category> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和类型查询分类列�?     */
    List<Category> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 根据父ID查询子分�?     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 删除分类
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据名称统计（排除指定ID）
     */
    int countByUserIdAndName(@Param("userId") Long userId, @Param("name") String name,
                             @Param("type") String type, @Param("parentId") Long parentId,
                             @Param("excludeId") Long excludeId);

    /**
     * 根据名称统计
     */
    int countByUserIdAndName(@Param("userId") Long userId, @Param("name") String name,
                             @Param("type") String type, @Param("parentId") Long parentId);

    /**
     * 搜索分类
     */
    List<Category> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 查询快速子分类
     */
    List<Category> selectQuickChildren(@Param("parentId") Long parentId, @Param("limit") int limit);

    /**
     * 更新子分类的父分类ID
     */
    int updateChildrenParentId(@Param("oldParentId") Long oldParentId, @Param("newParentId") Long newParentId);

    /**
     * 软删除
     */
    int softDeleteById(@Param("id") Long id);

    /**
     * 查询最近使用的分类
     */
    List<Category> selectRecentUsed(@Param("userId") Long userId, @Param("type") String type, @Param("limit") int limit);
}
