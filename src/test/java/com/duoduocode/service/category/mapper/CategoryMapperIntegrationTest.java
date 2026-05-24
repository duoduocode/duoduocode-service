package com.duoduocode.service.category.mapper;

import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CategoryMapperIntegrationTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_category_mapper_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private Category createCategory(String name, String type) {
        Category category = new Category();
        category.setUserId(testUserId);
        category.setName(name);
        category.setType(type);
        category.setIcon("🍔");
        category.setSortOrder(0);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        return category;
    }

    @Test
    void insert_shouldSuccess() {
        Category category = createCategory("测试分类_" + System.currentTimeMillis(), "expense");

        int result = categoryMapper.insert(category);

        assertEquals(1, result);
        assertNotNull(category.getId());
    }

    @Test
    void insert_shouldSetAllFields() {
        Category category = createCategory("完整字段_" + System.currentTimeMillis(), "expense");
        category.setParentId(null);

        categoryMapper.insert(category);

        Category saved = categoryMapper.selectById(category.getId());
        assertNotNull(saved);
        assertEquals(testUserId, saved.getUserId());
        assertEquals(category.getName(), saved.getName());
        assertEquals("expense", saved.getType());
    }

    @Test
    void updateById_shouldSuccess() {
        Category category = createCategory("更新前_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(category);

        category.setName("更新后");

        int result = categoryMapper.updateById(category);

        assertEquals(1, result);
        Category updated = categoryMapper.selectById(category.getId());
        assertEquals("更新后", updated.getName());
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        Category category = createCategory("不存在", "expense");
        category.setId(99999L);

        int result = categoryMapper.updateById(category);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnCategory() {
        Category category = createCategory("查询测试_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(category);

        Category result = categoryMapper.selectById(category.getId());

        assertNotNull(result);
        assertEquals(category.getName(), result.getName());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        Category result = categoryMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByUserId_shouldReturnAllCategories() {
        categoryMapper.insert(createCategory("分类1_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("分类2_" + System.currentTimeMillis(), "income"));
        categoryMapper.insert(createCategory("分类3_" + System.currentTimeMillis(), "expense"));

        List<Category> result = categoryMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void selectByUserId_shouldReturnEmptyListWhenNoCategory() {
        List<Category> result = categoryMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByUserIdAndType_shouldReturnFilteredList() {
        categoryMapper.insert(createCategory("支出1_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("支出2_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("收入_" + System.currentTimeMillis(), "income"));

        List<Category> expenseList = categoryMapper.selectByUserIdAndType(testUserId, "expense");
        List<Category> incomeList = categoryMapper.selectByUserIdAndType(testUserId, "income");

        assertEquals(2, expenseList.size());
        assertEquals(1, incomeList.size());
        expenseList.forEach(c -> assertEquals("expense", c.getType()));
        incomeList.forEach(c -> assertEquals("income", c.getType()));
    }

    @Test
    void selectByUserIdAndType_shouldReturnEmptyWhenNoMatch() {
        categoryMapper.insert(createCategory("支出_" + System.currentTimeMillis(), "expense"));

        List<Category> result = categoryMapper.selectByUserIdAndType(testUserId, "income");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectByParentId_shouldReturnChildren() {
        Category parent = createCategory("父分类_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(parent);

        Category child1 = createCategory("子分类1_" + System.currentTimeMillis(), "expense");
        child1.setParentId(parent.getId());
        categoryMapper.insert(child1);

        Category child2 = createCategory("子分类2_" + System.currentTimeMillis(), "expense");
        child2.setParentId(parent.getId());
        categoryMapper.insert(child2);

        List<Category> result = categoryMapper.selectByParentId(parent.getId());

        assertEquals(2, result.size());
    }

    @Test
    void selectByParentId_shouldReturnEmptyWhenNoChildren() {
        Category parent = createCategory("无子分类_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(parent);

        List<Category> result = categoryMapper.selectByParentId(parent.getId());

        assertEquals(0, result.size());
    }

    @Test
    void softDeleteById_shouldSuccess() {
        Category category = createCategory("软删除测试_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(category);
        Long categoryId = category.getId();

        int result = categoryMapper.softDeleteById(categoryId);

        assertEquals(1, result);

        Category deleted = categoryMapper.selectById(categoryId);
        assertNull(deleted);
    }

    @Test
    void softDeleteById_shouldReturnZeroWhenNotExist() {
        int result = categoryMapper.softDeleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void countByUserIdAndName_shouldReturnCount() {
        String uniqueName = "唯一名称_" + System.currentTimeMillis();
        categoryMapper.insert(createCategory(uniqueName, "expense"));

        int count = categoryMapper.countByUserIdAndName(testUserId, uniqueName, "expense", null, null);

        assertEquals(1, count);
    }

    @Test
    void countByUserIdAndName_shouldReturnZeroWhenNotExist() {
        int count = categoryMapper.countByUserIdAndName(testUserId, "不存在的名称", "expense", null, null);

        assertEquals(0, count);
    }

    @Test
    void countByUserIdAndName_withExcludeId_shouldExcludeCurrent() {
        String uniqueName = "排除测试_" + System.currentTimeMillis();
        Category category = createCategory(uniqueName, "expense");
        categoryMapper.insert(category);

        int count = categoryMapper.countByUserIdAndName(testUserId, uniqueName, "expense", null, category.getId());

        assertEquals(0, count);
    }

    @Test
    void countByUserIdAndName_withParentId_shouldWork() {
        String uniqueName = "父分类唯一名称_" + System.currentTimeMillis();
        Category parent = createCategory(uniqueName, "expense");
        categoryMapper.insert(parent);

        int count = categoryMapper.countByUserIdAndName(testUserId, uniqueName, "expense", null, null);

        assertEquals(1, count);
    }

    @Test
    void updateChildrenParentId_shouldUpdateChildren() {
        Category oldParent = createCategory("旧父分类_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(oldParent);

        Category newParent = createCategory("新父分类_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(newParent);

        Category child = createCategory("待迁移子分类_" + System.currentTimeMillis(), "expense");
        child.setParentId(oldParent.getId());
        categoryMapper.insert(child);

        int result = categoryMapper.updateChildrenParentId(oldParent.getId(), newParent.getId());

        assertTrue(result >= 1);
    }

    @Test
    void searchByKeyword_shouldReturnMatchedResults() {
        categoryMapper.insert(createCategory("餐饮支出_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("交通支出_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("工资收入_" + System.currentTimeMillis(), "income"));

        List<Category> result = categoryMapper.searchByKeyword(testUserId, "支出");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void searchByKeyword_shouldReturnEmptyWhenNoMatch() {
        categoryMapper.insert(createCategory("餐饮支出_" + System.currentTimeMillis(), "expense"));

        List<Category> result = categoryMapper.searchByKeyword(testUserId, "收入");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectQuickChildren_shouldReturnChildrenWithTransaction() {
        Category parent = createCategory("快速查询父分类_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(parent);

        for (int i = 0; i < 5; i++) {
            Category child = createCategory("快速子分类" + i + "_" + System.currentTimeMillis(), "expense");
            child.setParentId(parent.getId());
            categoryMapper.insert(child);
        }

        List<Category> result = categoryMapper.selectQuickChildren(parent.getId(), 10);

        assertNotNull(result);
    }

    @Test
    void selectQuickChildren_shouldReturnEmptyWhenNoChildren() {
        Category parent = createCategory("无子分类快速_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(parent);

        List<Category> result = categoryMapper.selectQuickChildren(parent.getId(), 10);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectRecentUsed_shouldReturnUsedCategories() {
        categoryMapper.insert(createCategory("常用分类1_" + System.currentTimeMillis(), "expense"));
        categoryMapper.insert(createCategory("常用分类2_" + System.currentTimeMillis(), "expense"));

        List<Category> result = categoryMapper.selectRecentUsed(testUserId, "expense", 10);

        assertNotNull(result);
    }

    @Test
    void selectRecentUsed_shouldReturnEmptyWhenNoUsage() {
        categoryMapper.insert(createCategory("未使用_" + System.currentTimeMillis(), "expense"));

        List<Category> result = categoryMapper.selectRecentUsed(testUserId, "expense", 10);

        assertNotNull(result);
    }

    @Test
    void categoryLifecycle_shouldWorkCorrectly() {
        Category category = createCategory("生命周期_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(category);

        Category saved = categoryMapper.selectById(category.getId());
        assertNotNull(saved);

        saved.setName("更新后_" + System.currentTimeMillis());
        int updateResult = categoryMapper.updateById(saved);
        assertEquals(1, updateResult);

        Category updated = categoryMapper.selectById(category.getId());
        assertEquals(saved.getName(), updated.getName());

        int deleteResult = categoryMapper.softDeleteById(category.getId());
        assertEquals(1, deleteResult);
    }

    @Test
    void multipleCategories_shouldBeIndependent() {
        Category category1 = createCategory("独立分类1_" + System.currentTimeMillis(), "expense");
        categoryMapper.insert(category1);

        Category category2 = createCategory("独立分类2_" + System.currentTimeMillis(), "income");
        categoryMapper.insert(category2);

        Category result1 = categoryMapper.selectById(category1.getId());
        Category result2 = categoryMapper.selectById(category2.getId());

        assertNotEquals(result1.getName(), result2.getName());
    }
}