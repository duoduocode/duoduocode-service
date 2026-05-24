package com.duoduocode.service.category.init;

import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CategoryDataInitializerIntegrationTest {

    @Autowired
    private CategoryDataInitializer categoryDataInitializer;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_cat_init_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    @Test
    void initForUser_shouldCreateAllDefaultCategories() {
        categoryDataInitializer.initForUser(testUserId);

        List<Category> categories = categoryMapper.selectByUserId(testUserId);
        assertNotNull(categories);
        assertTrue(categories.size() >= 80, "应至少有80条默认分类");

        long expenseCount = categories.stream().filter(c -> "expense".equals(c.getType())).count();
        long incomeCount = categories.stream().filter(c -> "income".equals(c.getType())).count();

        assertTrue(expenseCount >= 60, "支出分类应不少于60条");
        assertTrue(incomeCount >= 20, "收入分类应不少于20条");
    }

    @Test
    void initForUser_shouldBeIdempotent() {
        categoryDataInitializer.initForUser(testUserId);
        int firstCount = categoryMapper.selectByUserId(testUserId).size();

        categoryDataInitializer.initForUser(testUserId);
        int secondCount = categoryMapper.selectByUserId(testUserId).size();

        assertEquals(firstCount, secondCount, "第二次调用不应新增数据");
    }

    @Test
    void initForUser_shouldCreateCorrectTreeStructure() {
        categoryDataInitializer.initForUser(testUserId);

        List<Category> categories = categoryMapper.selectByUserId(testUserId);

        List<Category> parents = categories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());

        List<Category> children = categories.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.toList());

        assertTrue(parents.size() >= 15, "一级分类应不少于15个");
        assertTrue(children.size() >= 50, "二级分类应不少于50个");

        for (Category child : children) {
            assertNotNull(child.getParentId());
            boolean parentExists = parents.stream().anyMatch(p -> p.getId().equals(child.getParentId()));
            assertTrue(parentExists, "子分类 " + child.getName() + " 的父分类应存在");
        }
    }

    @Test
    void initForUser_shouldHaveCorrectExpenseParents() {
        categoryDataInitializer.initForUser(testUserId);

        List<Category> expenseParents = categoryMapper.selectByUserId(testUserId).stream()
                .filter(c -> c.getParentId() == null && "expense".equals(c.getType()))
                .collect(Collectors.toList());

        assertTrue(expenseParents.size() >= 12);

        List<String> expectedNames = java.util.Arrays.asList(
                "餐饮", "交通", "购物", "居住", "医疗", "娱乐",
                "教育", "通讯", "人情", "金融保险", "其他支出"
        );
        for (String name : expectedNames) {
            boolean found = expenseParents.stream().anyMatch(c -> name.equals(c.getName()));
            assertTrue(found, "应存在支出一级分类: " + name);
        }
    }

    @Test
    void initForUser_shouldHaveCorrectIncomeParents() {
        categoryDataInitializer.initForUser(testUserId);

        List<Category> incomeParents = categoryMapper.selectByUserId(testUserId).stream()
                .filter(c -> c.getParentId() == null && "income".equals(c.getType()))
                .collect(Collectors.toList());

        assertTrue(incomeParents.size() >= 5);

        List<String> expectedNames = java.util.Arrays.asList(
                "工资", "兼职副业", "投资收益", "红包礼金", "退款报销", "其他收入"
        );
        for (String name : expectedNames) {
            boolean found = incomeParents.stream().anyMatch(c -> name.equals(c.getName()));
            assertTrue(found, "应存在收入一级分类: " + name);
        }
    }

    @Test
    void initForUser_shouldHaveChildrenForFoodCategory() {
        categoryDataInitializer.initForUser(testUserId);

        Category foodParent = categoryMapper.selectByUserId(testUserId).stream()
                .filter(c -> "餐饮".equals(c.getName()) && "expense".equals(c.getType()))
                .findFirst().orElse(null);
        assertNotNull(foodParent);

        List<Category> foodChildren = categoryMapper.selectByUserId(testUserId).stream()
                .filter(c -> foodParent.getId().equals(c.getParentId()))
                .collect(Collectors.toList());

        assertTrue(foodChildren.size() >= 4);
        List<String> expected = java.util.Arrays.asList("早午晚餐", "零食水果", "外卖", "饮品咖啡", "聚餐宴请");
        for (String name : expected) {
            boolean found = foodChildren.stream().anyMatch(c -> name.equals(c.getName()));
            assertTrue(found, "餐饮应包含子分类: " + name);
        }
    }

    @Test
    void initForUser_differentUsersShouldGetSeparateCategories() {
        User user2 = new User();
        user2.setOpenid("test_openid_cat_init2_" + System.currentTimeMillis());
        user2.setGender(0);
        user2.setStatus(1);
        user2.setIsDeleted(false);
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user2);
        Long user2Id = user2.getId();

        categoryDataInitializer.initForUser(testUserId);
        categoryDataInitializer.initForUser(user2Id);

        List<Category> user1Categories = categoryMapper.selectByUserId(testUserId);
        List<Category> user2Categories = categoryMapper.selectByUserId(user2Id);

        assertTrue(user1Categories.size() >= 80);
        assertTrue(user2Categories.size() >= 80);

        for (Category c1 : user1Categories) {
            assertTrue(c1.getUserId().equals(testUserId));
        }
        for (Category c2 : user2Categories) {
            assertTrue(c2.getUserId().equals(user2Id));
        }
    }

    @Test
    void initForUser_shouldSetCorrectFields() {
        categoryDataInitializer.initForUser(testUserId);

        List<Category> categories = categoryMapper.selectByUserId(testUserId);

        for (Category c : categories) {
            assertEquals(testUserId, c.getUserId());
            assertNotNull(c.getName());
            assertNotNull(c.getType());
            assertNotNull(c.getIcon());
            assertFalse(c.getIsDeleted());
            assertNotNull(c.getCreatedAt());
        }

        List<Category> parents = categories.stream()
                .filter(c -> c.getParentId() == null)
                .collect(Collectors.toList());
        for (Category parent : parents) {
            assertTrue(parent.getSortOrder() >= 0);
        }
    }
}
