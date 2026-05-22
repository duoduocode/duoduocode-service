package com.duoduocode.service.category.dto;

/**
 * 分类迁移数据传输对象
 */
public class CategoryMigrateDTO {

    /**
     * 目标分类ID
     */
    private Long targetCategoryId;

    /**
     * 是否合并子分类
     */
    private Boolean mergeChildren;

    // Getters and Setters

    public Long getTargetCategoryId() {
        return targetCategoryId;
    }

    public void setTargetCategoryId(Long targetCategoryId) {
        this.targetCategoryId = targetCategoryId;
    }

    public Boolean getMergeChildren() {
        return mergeChildren;
    }

    public void setMergeChildren(Boolean mergeChildren) {
        this.mergeChildren = mergeChildren;
    }
}
