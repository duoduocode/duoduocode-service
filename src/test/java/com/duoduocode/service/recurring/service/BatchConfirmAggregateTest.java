package com.duoduocode.service.recurring.service;

import com.duoduocode.service.notification.service.NotificationService;
import com.duoduocode.service.recurring.dto.BatchConfirmVO;
import com.duoduocode.service.recurring.dto.ItemResult;
import com.duoduocode.service.recurring.entity.RecurringTemplate;
import com.duoduocode.service.transaction.dto.TransactionDTO;
import com.duoduocode.service.recurring.mapper.RecurringTemplateMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * R003 U305/U307：批量确认 DTO 构造与结果聚合。
 */
class BatchConfirmAggregateTest {

    private final RecurringTemplateService service =
            new RecurringTemplateService(mock(RecurringTemplateMapper.class),
                    mock(NotificationService.class));

    private RecurringTemplate tpl() {
        RecurringTemplate t = new RecurringTemplate();
        t.setId(42L);
        t.setUserId(9L);
        t.setName("PyTest房租周期账");
        t.setType("expense");
        t.setAmount(new BigDecimal("2500.00"));
        t.setCategoryId(7L);
        t.setAccountId(3L);
        t.setDescription("月度房租");
        t.setFrequency("monthly");
        t.setNextTriggerDate(LocalDate.of(2026, 8, 8));
        return t;
    }

    @Test
    void U307_buildConfirmDTO_字段映射() {
        TransactionDTO dto = service.buildConfirmDTO(tpl());
        assertEquals(new BigDecimal("2500.00"), dto.getAmount());
        assertEquals("expense", dto.getTransactionType());
        assertEquals(7L, dto.getCategoryId());
        assertEquals(3L, dto.getAccountId());
        assertEquals(LocalDate.of(2026, 8, 8), dto.getDate());
        assertEquals(42L, dto.getSourceTemplateId());
        assertEquals("simple", dto.getMode());
        assertNotNull(dto.getDescription());
        assertTrue(dto.getDescription().contains("PyTest房租周期账"),
                "描述应含模板名: " + dto.getDescription());
    }

    @Test
    void U305_summarize_全成功() {
        BatchConfirmVO vo = BatchConfirmVO.summarize(
                List.of(ItemResult.ok(1L), ItemResult.ok(2L), ItemResult.ok(3L)));
        assertEquals(3, vo.getSuccessCount());
        assertEquals(0, vo.getFailCount());
        assertTrue(vo.getFailures().isEmpty());
    }

    @Test
    void U305_summarize_部分失败() {
        BatchConfirmVO vo = BatchConfirmVO.summarize(
                List.of(ItemResult.ok(1L), ItemResult.fail(2L, "分类不存在"),
                        ItemResult.fail(3L, "模板不存在")));
        assertEquals(1, vo.getSuccessCount());
        assertEquals(2, vo.getFailCount());
        assertEquals(2, vo.getFailures().size());
        assertEquals(2L, vo.getFailures().get(0).getId());
        assertEquals("分类不存在", vo.getFailures().get(0).getReason());
    }

    @Test
    void U305_summarize_全失败_聚合仍成功() {
        BatchConfirmVO vo = BatchConfirmVO.summarize(
                List.of(ItemResult.fail(1L, "a"), ItemResult.fail(2L, "b")));
        assertEquals(0, vo.getSuccessCount());
        assertEquals(2, vo.getFailCount());
        assertEquals(2, vo.getFailures().size());
    }
}
