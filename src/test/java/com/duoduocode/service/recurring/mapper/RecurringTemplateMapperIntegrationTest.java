package com.duoduocode.service.recurring.mapper;

import com.duoduocode.service.entity.User;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RecurringTemplateMapperIntegrationTest {

    @Autowired
    private RecurringTemplateMapper recurringTemplateMapper;

    @Autowired
    private UserMapper userMapper;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_recurring_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();
    }

    private RecurringTemplate createTemplate(String name, String frequency) {
        RecurringTemplate template = new RecurringTemplate();
        template.setUserId(testUserId);
        template.setName(name);
        template.setAmount(new BigDecimal("100.00"));
        template.setType("expense");
        template.setFrequency(frequency);
        template.setStartDate(LocalDate.of(2026, 5, 1));
        template.setDescription("");
        template.setExecutedCount(0);
        template.setNextTriggerDate(LocalDate.of(2026, 5, 15));
        template.setStatus("active");
        template.setIsDeleted(0);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return template;
    }

    @Test
    void insert_shouldSuccess() {
        RecurringTemplate template = createTemplate("测试模板_daily", "daily");

        int result = recurringTemplateMapper.insert(template);

        assertEquals(1, result);
        assertNotNull(template.getId());
    }

    @Test
    void updateById_shouldSuccess() {
        RecurringTemplate template = createTemplate("原始名称", "daily");
        recurringTemplateMapper.insert(template);

        template.setName("更新后的名称");
        template.setAmount(new BigDecimal("200.00"));
        int result = recurringTemplateMapper.updateById(template);

        assertEquals(1, result);

        RecurringTemplate updated = recurringTemplateMapper.selectById(template.getId());
        assertEquals("更新后的名称", updated.getName());
        assertEquals(0, new BigDecimal("200.00").compareTo(updated.getAmount()));
    }

    @Test
    void updateById_shouldReturnZeroWhenNotExist() {
        RecurringTemplate template = createTemplate("测试模板", "daily");
        template.setId(99999L);

        int result = recurringTemplateMapper.updateById(template);

        assertEquals(0, result);
    }

    @Test
    void deleteById_shouldSuccess() {
        RecurringTemplate template = createTemplate("待删除模板", "daily");
        recurringTemplateMapper.insert(template);

        int result = recurringTemplateMapper.deleteById(template.getId());

        assertEquals(1, result);
    }

    @Test
    void deleteById_shouldReturnZeroWhenNotExist() {
        int result = recurringTemplateMapper.deleteById(99999L);

        assertEquals(0, result);
    }

    @Test
    void selectById_shouldReturnTemplate() {
        RecurringTemplate template = createTemplate("查询模板", "weekly");
        recurringTemplateMapper.insert(template);

        RecurringTemplate result = recurringTemplateMapper.selectById(template.getId());

        assertNotNull(result);
        assertEquals(template.getId(), result.getId());
        assertEquals("weekly", result.getFrequency());
    }

    @Test
    void selectById_shouldReturnNullWhenNotExist() {
        RecurringTemplate result = recurringTemplateMapper.selectById(99999L);

        assertNull(result);
    }

    @Test
    void selectByUserId_shouldReturnAllTemplates() {
        recurringTemplateMapper.insert(createTemplate("模板1_daily", "daily"));
        recurringTemplateMapper.insert(createTemplate("模板2_weekly", "weekly"));
        recurringTemplateMapper.insert(createTemplate("模板3_monthly", "monthly"));

        List<RecurringTemplate> result = recurringTemplateMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void selectByUserId_shouldReturnEmptyListWhenNoTemplate() {
        List<RecurringTemplate> result = recurringTemplateMapper.selectByUserId(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectDueTemplates_shouldReturnDueTemplates() {
        RecurringTemplate template = createTemplate("到期模板", "daily");
        template.setNextTriggerDate(LocalDate.of(2026, 5, 10));
        template.setStatus("active");
        recurringTemplateMapper.insert(template);

        List<RecurringTemplate> result = recurringTemplateMapper.selectDueTemplates(
                testUserId, LocalDate.of(2026, 5, 15));

        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    void selectDueTemplates_shouldReturnEmptyWhenNoDueTemplate() {
        RecurringTemplate template = createTemplate("未来模板", "daily");
        template.setNextTriggerDate(LocalDate.of(2026, 6, 1));
        recurringTemplateMapper.insert(template);

        List<RecurringTemplate> result = recurringTemplateMapper.selectDueTemplates(
                testUserId, LocalDate.of(2026, 5, 15));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void selectDueTemplates_shouldNotReturnPausedTemplate() {
        RecurringTemplate template = createTemplate("暂停模板", "daily");
        template.setNextTriggerDate(LocalDate.of(2026, 5, 10));
        template.setStatus("paused");
        recurringTemplateMapper.insert(template);

        List<RecurringTemplate> result = recurringTemplateMapper.selectDueTemplates(
                testUserId, LocalDate.of(2026, 5, 15));

        assertNotNull(result);
        assertTrue(result.stream().noneMatch(t -> t.getName().equals("暂停模板")));
    }

    @Test
    void selectDueTemplates_shouldRespectEndDate() {
        RecurringTemplate template = createTemplate("已过期模板", "daily");
        template.setNextTriggerDate(LocalDate.of(2026, 5, 10));
        template.setEndDate(LocalDate.of(2026, 5, 5));
        recurringTemplateMapper.insert(template);

        List<RecurringTemplate> result = recurringTemplateMapper.selectDueTemplates(
                testUserId, LocalDate.of(2026, 5, 15));

        assertNotNull(result);
        assertTrue(result.stream().noneMatch(t -> t.getName().equals("已过期模板")));
    }

    @Test
    void selectDueTemplates_shouldRespectMaxCount() {
        RecurringTemplate template = createTemplate("已达上限模板", "daily");
        template.setNextTriggerDate(LocalDate.of(2026, 5, 10));
        template.setMaxCount(5);
        template.setExecutedCount(5);
        recurringTemplateMapper.insert(template);

        List<RecurringTemplate> result = recurringTemplateMapper.selectDueTemplates(
                testUserId, LocalDate.of(2026, 5, 15));

        assertNotNull(result);
        assertTrue(result.stream().noneMatch(t -> t.getName().equals("已达上限模板")));
    }

    @Test
    void updateStatus_shouldSuccess() {
        RecurringTemplate template = createTemplate("待暂停模板", "daily");
        recurringTemplateMapper.insert(template);

        int result = recurringTemplateMapper.updateStatus(template.getId(), "paused");

        assertEquals(1, result);

        RecurringTemplate updated = recurringTemplateMapper.selectById(template.getId());
        assertEquals("paused", updated.getStatus());
    }

    @Test
    void updateStatus_shouldReturnZeroWhenNotExist() {
        int result = recurringTemplateMapper.updateStatus(99999L, "paused");

        assertEquals(0, result);
    }

    @Test
    void updateExecution_shouldSuccess() {
        RecurringTemplate template = createTemplate("执行更新模板", "daily");
        recurringTemplateMapper.insert(template);

        int result = recurringTemplateMapper.updateExecution(
                template.getId(),
                3,
                LocalDate.of(2026, 6, 1),
                LocalDateTime.now()
        );

        assertEquals(1, result);

        RecurringTemplate updated = recurringTemplateMapper.selectById(template.getId());
        assertEquals(3, updated.getExecutedCount());
        assertEquals(LocalDate.of(2026, 6, 1), updated.getNextTriggerDate());
        assertNotNull(updated.getLastTriggeredAt());
    }

    @Test
    void updateExecution_shouldReturnZeroWhenNotExist() {
        int result = recurringTemplateMapper.updateExecution(
                99999L,
                3,
                LocalDate.of(2026, 6, 1),
                LocalDateTime.now()
        );

        assertEquals(0, result);
    }

    @Test
    void templateLifecycle_shouldWorkCorrectly() {
        RecurringTemplate template = createTemplate("生命周期模板", "monthly");
        recurringTemplateMapper.insert(template);

        RecurringTemplate saved = recurringTemplateMapper.selectById(template.getId());
        assertNotNull(saved);

        saved.setName("更新后的生命周期模板");
        saved.setStatus("paused");
        recurringTemplateMapper.updateById(saved);

        RecurringTemplate updated = recurringTemplateMapper.selectById(template.getId());
        assertEquals("更新后的生命周期模板", updated.getName());
        assertEquals("paused", updated.getStatus());

        recurringTemplateMapper.updateStatus(template.getId(), "active");
        RecurringTemplate reactivated = recurringTemplateMapper.selectById(template.getId());
        assertEquals("active", reactivated.getStatus());

        int deleteResult = recurringTemplateMapper.deleteById(template.getId());
        assertEquals(1, deleteResult);
    }

    @Test
    void multipleTemplates_shouldBeIndependent() {
        RecurringTemplate template1 = createTemplate("独立模板1", "daily");
        RecurringTemplate template2 = createTemplate("独立模板2", "weekly");
        RecurringTemplate template3 = createTemplate("独立模板3", "monthly");
        recurringTemplateMapper.insert(template1);
        recurringTemplateMapper.insert(template2);
        recurringTemplateMapper.insert(template3);

        RecurringTemplate result1 = recurringTemplateMapper.selectById(template1.getId());
        RecurringTemplate result2 = recurringTemplateMapper.selectById(template2.getId());
        RecurringTemplate result3 = recurringTemplateMapper.selectById(template3.getId());

        assertNotEquals(result1.getFrequency(), result2.getFrequency());
        assertNotEquals(result2.getFrequency(), result3.getFrequency());
        assertEquals("daily", result1.getFrequency());
        assertEquals("weekly", result2.getFrequency());
        assertEquals("monthly", result3.getFrequency());
    }
}