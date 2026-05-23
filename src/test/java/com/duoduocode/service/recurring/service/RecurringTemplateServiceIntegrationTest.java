package com.duoduocode.service.recurring.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.recurring.dto.RecurringTemplateDTO;
import com.duoduocode.service.recurring.dto.RecurringTemplateVO;
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
class RecurringTemplateServiceIntegrationTest {

    @Autowired
    private RecurringTemplateService recurringTemplateService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AccountMapper accountMapper;

    private Long testUserId;
    private Long testAccountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setOpenid("test_openid_recurring_service_" + System.currentTimeMillis());
        user.setGender(0);
        user.setStatus(1);
        user.setIsDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        testUserId = user.getId();

        Account account = new Account();
        account.setUserId(testUserId);
        account.setName("测试账户_" + System.currentTimeMillis());
        account.setType("asset");
        account.setIcon("💰");
        account.setColor("#FF5733");
        account.setInitialBalance(new BigDecimal("10000.00"));
        account.setCreditLimit(BigDecimal.ZERO);
        account.setIncludeInNetWorth(true);
        account.setAllowTransfer(true);
        account.setEnableAlert(false);
        account.setAlertThreshold(new BigDecimal("0.8"));
        account.setSortOrder(0);
        account.setIsDeleted(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        testAccountId = account.getId();
    }

    private RecurringTemplateDTO createTemplateDTO(String name, String type, String frequency) {
        RecurringTemplateDTO dto = new RecurringTemplateDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setAmount(new BigDecimal("100.00"));
        dto.setFrequency(frequency);
        dto.setAccountId(testAccountId);
        dto.setStartDate(LocalDate.of(2026, 5, 1));
        return dto;
    }

    @Test
    void getTemplateList_shouldReturnEmptyListWhenNoTemplate() {
        List<RecurringTemplateVO> result = recurringTemplateService.getTemplateList(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getTemplateList_shouldReturnAllTemplates() {
        recurringTemplateService.createTemplate(testUserId, createTemplateDTO("模板1", "expense", "daily"));
        recurringTemplateService.createTemplate(testUserId, createTemplateDTO("模板2", "income", "weekly"));

        List<RecurringTemplateVO> result = recurringTemplateService.getTemplateList(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getTemplateDetail_shouldReturnTemplate() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("详情模板", "expense", "monthly"));

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);

        assertNotNull(result);
        assertEquals(templateId, result.getId());
        assertEquals("详情模板", result.getName());
        assertEquals("monthly", result.getFrequency());
    }

    @Test
    void getTemplateDetail_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.getTemplateDetail(99999L);
        });
    }

    @Test
    void createTemplate_shouldSuccessWithValidData() {
        RecurringTemplateDTO dto = createTemplateDTO("新建模板", "expense", "daily");
        dto.setDayOfWeek(1);
        dto.setDayOfMonth(15);
        dto.setMonthOfYear(6);
        dto.setMaxCount(10);

        Long templateId = recurringTemplateService.createTemplate(testUserId, dto);

        assertNotNull(templateId);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("新建模板", result.getName());
        assertEquals("expense", result.getType());
        assertEquals("daily", result.getFrequency());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getAmount()));
        assertEquals("active", result.getStatus());
        assertEquals(0, result.getExecutedCount());
    }

    @Test
    void createTemplate_shouldSetCorrectTypeName() {
        RecurringTemplateDTO dto = createTemplateDTO("支出模板", "expense", "daily");
        Long templateId = recurringTemplateService.createTemplate(testUserId, dto);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("支出", result.getTypeName());
    }

    @Test
    void createTemplate_shouldSetCorrectFrequencyName() {
        RecurringTemplateDTO dto = createTemplateDTO("每周模板", "expense", "weekly");
        Long templateId = recurringTemplateService.createTemplate(testUserId, dto);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("每周", result.getFrequencyName());
    }

    @Test
    void updateTemplate_shouldSuccess() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("待更新模板", "expense", "daily"));

        RecurringTemplateDTO updateDto = createTemplateDTO("已更新模板", "income", "weekly");
        updateDto.setAmount(new BigDecimal("200.00"));
        recurringTemplateService.updateTemplate(templateId, updateDto);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("已更新模板", result.getName());
        assertEquals("income", result.getType());
        assertEquals("weekly", result.getFrequency());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getAmount()));
    }

    @Test
    void updateTemplate_shouldThrowExceptionWhenNotExist() {
        RecurringTemplateDTO dto = createTemplateDTO("不存在模板", "expense", "daily");

        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.updateTemplate(99999L, dto);
        });
    }

    @Test
    void deleteTemplate_shouldSuccess() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("待删除模板", "expense", "daily"));

        recurringTemplateService.deleteTemplate(templateId);

        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.getTemplateDetail(templateId);
        });
    }

    @Test
    void getDueTemplates_shouldReturnDueTemplates() {
        RecurringTemplateDTO dto = createTemplateDTO("到期模板", "expense", "daily");
        dto.setStartDate(LocalDate.of(2026, 5, 1));
        recurringTemplateService.createTemplate(testUserId, dto);

        List<RecurringTemplateVO> result = recurringTemplateService.getDueTemplates(testUserId);

        assertNotNull(result);
    }

    @Test
    void getDueTemplates_shouldReturnEmptyWhenNoDueTemplate() {
        RecurringTemplateDTO dto = createTemplateDTO("未来模板", "expense", "yearly");
        dto.setStartDate(LocalDate.of(2099, 1, 1));
        recurringTemplateService.createTemplate(testUserId, dto);

        List<RecurringTemplateVO> result = recurringTemplateService.getDueTemplates(testUserId);

        assertNotNull(result);
    }

    @Test
    void triggerTemplate_shouldIncrementExecutedCount() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("触发模板", "expense", "daily"));

        RecurringTemplateVO beforeTrigger = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals(0, beforeTrigger.getExecutedCount());

        recurringTemplateService.triggerTemplate(templateId);

        RecurringTemplateVO afterTrigger = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals(1, afterTrigger.getExecutedCount());
        assertNotNull(afterTrigger.getLastTriggeredAt());
    }

    @Test
    void triggerTemplate_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.triggerTemplate(99999L);
        });
    }

    @Test
    void pauseTemplate_shouldChangeStatusToPaused() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("暂停模板", "expense", "daily"));

        recurringTemplateService.pauseTemplate(templateId);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("paused", result.getStatus());
    }

    @Test
    void pauseTemplate_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.pauseTemplate(99999L);
        });
    }

    @Test
    void resumeTemplate_shouldChangeStatusToActive() {
        Long templateId = recurringTemplateService.createTemplate(
                testUserId, createTemplateDTO("恢复模板", "expense", "daily"));
        recurringTemplateService.pauseTemplate(templateId);

        recurringTemplateService.resumeTemplate(templateId);

        RecurringTemplateVO result = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("active", result.getStatus());
    }

    @Test
    void resumeTemplate_shouldThrowExceptionWhenNotExist() {
        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.resumeTemplate(99999L);
        });
    }

    @Test
    void fullTemplateLifecycle_shouldWorkCorrectly() {
        RecurringTemplateDTO dto = createTemplateDTO("完整生命周期", "expense", "monthly");
        dto.setMaxCount(3);

        Long templateId = recurringTemplateService.createTemplate(testUserId, dto);

        RecurringTemplateVO template = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("完整生命周期", template.getName());
        assertEquals("active", template.getStatus());
        assertEquals(0, template.getExecutedCount());

        recurringTemplateService.updateTemplate(templateId, createTemplateDTO("更新后生命周期", "income", "weekly"));

        RecurringTemplateVO updated = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("更新后生命周期", updated.getName());
        assertEquals("income", updated.getType());

        recurringTemplateService.pauseTemplate(templateId);
        RecurringTemplateVO paused = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("paused", paused.getStatus());

        recurringTemplateService.resumeTemplate(templateId);
        RecurringTemplateVO resumed = recurringTemplateService.getTemplateDetail(templateId);
        assertEquals("active", resumed.getStatus());

        recurringTemplateService.deleteTemplate(templateId);
        assertThrows(BusinessException.class, () -> {
            recurringTemplateService.getTemplateDetail(templateId);
        });
    }
}