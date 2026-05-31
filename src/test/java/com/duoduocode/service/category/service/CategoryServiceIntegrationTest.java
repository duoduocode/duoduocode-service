package com.duoduocode.service.category.service;

import com.duoduocode.service.category.dto.CategoryDTO;
import com.duoduocode.service.category.dto.CategoryMigrateDTO;
import com.duoduocode.service.category.dto.CategoryTreeVO;
import com.duoduocode.service.category.dto.CategoryVO;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
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
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_category_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private CategoryDTO createDto(String name, String type) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setSortOrder(0);
        return dto;
    }

    @Test
    void getCategoryList_shouldReturnTreeStructure() {
        categoryService.createCategory(testUserId, createDto("餐饮", "expense"));
        categoryService.createCategory(testUserId, createDto("购物", "expense"));

        List<CategoryTreeVO> result = categoryService.getCategoryList(testUserId, "expense");

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void getCategoryList_shouldReturnAllTypesWhenTypeIsNull() {
        categoryService.createCategory(testUserId, createDto("工资", "income"));
        categoryService.createCategory(testUserId, createDto("餐饮", "expense"));

        List<CategoryTreeVO> result = categoryService.getCategoryList(testUserId, null);

        assertNotNull(result);
        assertTrue(result.size() >= 2);
    }

    @Test
    void getRecentCategories_shouldReturnRecentUsed() {
        categoryService.createCategory(testUserId, createDto("餐饮", "expense"));
        categoryService.createCategory(testUserId, createDto("购物", "expense"));
        categoryService.createCategory(testUserId, createDto("交通", "expense"));

        List<CategoryVO> result = categoryService.getRecentCategories(testUserId, "expense", 3);

        assertNotNull(result);
    }

    @Test
    void searchCategories_shouldReturnMatchedResults() {
        categoryService.createCategory(testUserId, createDto("餐饮", "expense"));

        List<CategoryVO> result = categoryService.searchCategories(testUserId, "餐饮");

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void searchCategories_shouldReturnEmptyWhenKeywordIsEmpty() {
        List<CategoryVO> result = categoryService.searchCategories(testUserId, "");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getQuickChildren_shouldReturnChildren() {
        CategoryDTO parentDto = createDto("餐饮", "expense");
        Long parentId = categoryService.createCategory(testUserId, parentDto);

        CategoryDTO childDto = createDto("午餐", "expense");
        childDto.setParentId(parentId);
        categoryService.createCategory(testUserId, childDto);

        List<CategoryTreeVO> result = categoryService.getQuickChildren(parentId);

        assertNotNull(result);
    }

    @Test
    void getQuickChildren_shouldThrowExceptionWhenParentNotExist() {
        assertThrows(BusinessException.class, () -> {
            categoryService.getQuickChildren(99999L);
        });
    }

    @Test
    void getCategoryUsageCount_shouldReturnCount() {
        Long categoryId = categoryService.createCategory(testUserId, createDto("使用统计", "expense"));

        Long count = categoryService.getCategoryUsageCount(categoryId);

        assertNotNull(count);
    }

    @Test
    void createCategory_shouldSuccess() {
        CategoryDTO dto = createDto("餐饮_" + System.currentTimeMillis(), "expense");
        dto.setSortOrder(0);

        Long categoryId = categoryService.createCategory(testUserId, dto);

        assertNotNull(categoryId);
    }

    @Test
    void createCategory_shouldThrowExceptionWhenNameEmpty() {
        CategoryDTO dto = createDto("", "expense");

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(testUserId, dto);
        });
    }

    @Test
    void createCategory_shouldThrowExceptionWhenTypeEmpty() {
        CategoryDTO dto = createDto("测试分类", "");

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(testUserId, dto);
        });
    }

    @Test
    void createCategory_shouldThrowExceptionWhenTypeInvalid() {
        CategoryDTO dto = createDto("测试分类", "invalid_type");

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(testUserId, dto);
        });
    }

    @Test
    void createCategory_shouldThrowExceptionWhenNameDuplicate() {
        String uniqueName = "重复分类_" + System.currentTimeMillis();
        categoryService.createCategory(testUserId, createDto(uniqueName, "expense"));

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(testUserId, createDto(uniqueName, "expense"));
        });
    }

    @Test
    void createCategory_shouldCreateChildCategory() {
        CategoryDTO parentDto = createDto("餐饮", "expense");
        Long parentId = categoryService.createCategory(testUserId, parentDto);

        CategoryDTO childDto = createDto("午餐", "expense");
        childDto.setParentId(parentId);
        Long childId = categoryService.createCategory(testUserId, childDto);

        assertNotNull(childId);
    }

    @Test
    void createCategory_shouldThrowExceptionWhenChildTypeMismatch() {
        CategoryDTO parentDto = createDto("餐饮", "expense");
        Long parentId = categoryService.createCategory(testUserId, parentDto);

        CategoryDTO childDto = createDto("工资", "income");
        childDto.setParentId(parentId);

        assertThrows(BusinessException.class, () -> {
            categoryService.createCategory(testUserId, childDto);
        });
    }

    @Test
    void updateCategory_shouldSuccess() {
        Long categoryId = categoryService.createCategory(testUserId, createDto("更新前", "expense"));

        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName("更新后");
        updateDto.setIcon("🍜");
        updateDto.setColor("#FF0000");

        assertDoesNotThrow(() -> {
            categoryService.updateCategory(testUserId, categoryId, updateDto);
        });
    }

    @Test
    void updateCategory_shouldThrowExceptionWhenCategoryNotExist() {
        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName("不存在");

        assertThrows(BusinessException.class, () -> {
            categoryService.updateCategory(testUserId, 99999L, updateDto);
        });
    }

    @Test
    void updateCategory_shouldThrowExceptionWhenNameDuplicate() {
        String uniqueName = "重复_" + System.currentTimeMillis();
        categoryService.createCategory(testUserId, createDto(uniqueName, "expense"));

        Long categoryId2 = categoryService.createCategory(testUserId, createDto("另一个", "expense"));

        CategoryDTO updateDto = new CategoryDTO();
        updateDto.setName(uniqueName);

        assertThrows(BusinessException.class, () -> {
            categoryService.updateCategory(testUserId, categoryId2, updateDto);
        });
    }

    @Test
    void deleteCategory_shouldSuccess() {
        Long categoryId = categoryService.createCategory(testUserId, createDto("待删除", "expense"));

        assertDoesNotThrow(() -> {
            categoryService.deleteCategory(testUserId, categoryId, null);
        });
    }

    @Test
    void deleteCategory_shouldThrowExceptionWhenCategoryNotExist() {
        assertThrows(BusinessException.class, () -> {
            categoryService.deleteCategory(testUserId, 99999L, null);
        });
    }

    @Test
    void deleteCategory_withMigrateTo_shouldMigrateChildren() {
        CategoryDTO parentDto = createDto("餐饮", "expense");
        Long parentId = categoryService.createCategory(testUserId, parentDto);

        CategoryDTO childDto = createDto("午餐", "expense");
        childDto.setParentId(parentId);
        categoryService.createCategory(testUserId, childDto);

        CategoryDTO targetDto = createDto("娱乐", "expense");
        Long targetId = categoryService.createCategory(testUserId, targetDto);

        assertDoesNotThrow(() -> {
            categoryService.deleteCategory(testUserId, parentId, targetId);
        });
    }

    @Test
    void getMigrationOptions_shouldReturnOptions() {
        categoryService.createCategory(testUserId, createDto("分类A", "expense"));
        Long categoryBId = categoryService.createCategory(testUserId, createDto("分类B", "expense"));

        List<CategoryVO> options = categoryService.getMigrationOptions(testUserId, categoryBId);

        assertNotNull(options);
    }

    @Test
    void getMigrationOptions_shouldThrowExceptionWhenCategoryNotExist() {
        assertThrows(BusinessException.class, () -> {
            categoryService.getMigrationOptions(testUserId, 99999L);
        });
    }

    @Test
    void migrateCategory_shouldSuccess() {
        Long categoryAId = categoryService.createCategory(testUserId, createDto("分类A_" + System.currentTimeMillis(), "expense"));
        Long categoryBId = categoryService.createCategory(testUserId, createDto("分类B_" + System.currentTimeMillis(), "expense"));

        CategoryMigrateDTO migrateDto = new CategoryMigrateDTO();
        migrateDto.setTargetCategoryId(categoryBId);
        migrateDto.setMergeChildren(false);

        assertDoesNotThrow(() -> {
            categoryService.migrateCategory(categoryAId, migrateDto);
        });
    }

    @Test
    void migrateCategory_shouldThrowExceptionWhenSourceNotExist() {
        Long targetId = categoryService.createCategory(testUserId, createDto("目标", "expense"));

        CategoryMigrateDTO migrateDto = new CategoryMigrateDTO();
        migrateDto.setTargetCategoryId(targetId);

        assertThrows(BusinessException.class, () -> {
            categoryService.migrateCategory(99999L, migrateDto);
        });
    }

    @Test
    void migrateCategory_shouldThrowExceptionWhenTargetNotExist() {
        Long sourceId = categoryService.createCategory(testUserId, createDto("源", "expense"));

        CategoryMigrateDTO migrateDto = new CategoryMigrateDTO();
        migrateDto.setTargetCategoryId(99999L);

        assertThrows(BusinessException.class, () -> {
            categoryService.migrateCategory(sourceId, migrateDto);
        });
    }

    @Test
    void migrateCategory_shouldThrowExceptionWhenTypeMismatch() {
        Long incomeId = categoryService.createCategory(testUserId, createDto("收入", "income"));
        Long expenseId = categoryService.createCategory(testUserId, createDto("支出", "expense"));

        CategoryMigrateDTO migrateDto = new CategoryMigrateDTO();
        migrateDto.setTargetCategoryId(expenseId);

        assertThrows(BusinessException.class, () -> {
            categoryService.migrateCategory(incomeId, migrateDto);
        });
    }
}