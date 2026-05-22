package com.duoduocode.service.category.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树形视图对象
 */
public class CategoryTreeVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 图标emoji
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 子分类列表
     */
    private List<CategoryTreeVO> children = new ArrayList<>();

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<CategoryTreeVO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeVO> children) {
        this.children = children;
    }
}
