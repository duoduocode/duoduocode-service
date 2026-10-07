package com.duoduocode.service.budget.service;

import com.duoduocode.service.budget.dto.BatchCarryoverDTO;
import com.duoduocode.service.budget.mapper.BudgetCarryoverMapper;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.transaction.mapper.EntryMapper;
import com.duoduocode.service.budget.mapper.DailyBudgetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * R003 U308：批量结转校验（fromMonth 上月规则 / 金额超限项失败）。
 */
@ExtendWith(MockitoExtension.class)
class BudgetCarryoverBatchValidationTest {

    @Mock
    private BudgetCarryoverMapper budgetCarryoverMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private EntryMapper entryMapper;
    @Mock
    private DailyBudgetMapper dailyBudgetMapper;

    private BudgetCarryoverService buildService() {
        return new BudgetCarryoverService(budgetCarryoverMapper, categoryMapper,
                entryMapper, dailyBudgetMapper);
    }

    private String lastMonth() {
        return YearMonth.from(LocalDate.now().minusMonths(1)).toString();
    }

    private BatchCarryoverDTO dto(String fromMonth, BigDecimal amount) {
        BatchCarryoverDTO dto = new BatchCarryoverDTO();
        BatchCarryoverDTO.Item item = new BatchCarryoverDTO.Item();
        item.setCategoryId(7L);
        item.setFromMonth(fromMonth);
        item.setAmount(amount);
        dto.setItems(List.of(item));
        return dto;
    }

    @Test
    void U308_fromMonth_仅允许上一个月() {
        String prev2 = YearMonth.from(LocalDate.now().minusMonths(2)).toString();
        String cur = YearMonth.from(LocalDate.now()).toString();
        assertThrows(BusinessException.class,
                () -> buildService().carryoverBatch(9L, dto(prev2, BigDecimal.ONE)));
        assertThrows(BusinessException.class,
                () -> buildService().carryoverBatch(9L, dto(cur, BigDecimal.ONE)));
        assertThrows(BusinessException.class,
                () -> buildService().carryoverBatch(9L, dto("not-a-month", BigDecimal.ONE)));
    }

    @Test
    void U308_金额超可结余_该项失败不中断() {
        BudgetCarryoverService spy = spy(buildService());
        doReturn(new BigDecimal("200.00"))
                .when(spy).calculateCarryoverAmount(9L, 7L, lastMonth());
        com.duoduocode.service.budget.dto.BatchCarryoverVO vo =
                spy.carryoverBatch(9L, dto(lastMonth(), new BigDecimal("9999.00")));
        assertEquals(0, vo.getSuccessCount());
        assertEquals(1, vo.getFailCount());
        assertFalse(vo.getFailures().isEmpty());
        verify(spy, never()).carryoverBudget(anyLong(), any());
    }

    @Test
    void U308_合法金额_该项成功() {
        BudgetCarryoverService spy = spy(buildService());
        doReturn(new BigDecimal("200.00"))
                .when(spy).calculateCarryoverAmount(9L, 7L, lastMonth());
        doReturn(11L).when(spy).carryoverBudget(anyLong(), any());
        com.duoduocode.service.budget.dto.BatchCarryoverVO vo =
                spy.carryoverBatch(9L, dto(lastMonth(), new BigDecimal("200.00")));
        assertEquals(1, vo.getSuccessCount());
        assertEquals(0, vo.getFailCount());
    }
}
