package com.duoduocode.service.category.dto;

/**
 * 分类数据传输对象
 */
public class CategoryDTO {

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类类型：expense-支出 income-收入
     */
    private String type;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 图标emoji
     */
    private String icon;

    /**
     * 颜色（十六进制）
     */
    private String color;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
