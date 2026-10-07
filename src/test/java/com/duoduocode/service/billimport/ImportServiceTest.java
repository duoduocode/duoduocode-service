package com.duoduocode.service.billimport;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.billimport.ImportBatch;
import com.duoduocode.service.billimport.mapper.ImportBatchMapper;
import com.duoduocode.service.transaction.dto.TransactionDTO;
import com.duoduocode.service.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 导入服务（U06）：账户匹配/创建、批内去重、未知方式归现金、DTO 构造、判重时点。
 */
@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private AccountMapper accountMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private com.duoduocode.service.transaction.mapper.TransactionMapper transactionMapper;
    @Mock
    private TransactionService transactionService;
    @Mock
    private ImportBatchMapper importBatchMapper;

    private BillAccountMatcher matcher() {
        return new BillAccountMatcher(accountMapper);
    }

    private static List<Account> accounts(Account... as) {
        List<Account> list = new ArrayList<>();
        for (Account a : as) list.add(a);
        return list;
    }

    private static Account account(Long id, String name, String type) {
        Account a = new Account();
        a.setId(id);
        a.setName(name);
        a.setType(type);
        return a;
    }

    private static Category category(Long id, String name, String type) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setType(type);
        return c;
    }

    @Test
    void 账户匹配_同名命中() {
        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(
                account(3L, "现金", "asset"),
                account(4L, "招商银行(1234)", "asset")));
        BillAccountMatcher m = matcher();
        BillAccountMatcher.MatchResult r = m.match(1L, "招商银行(1234)", new ArrayList<>());
        assertEquals(4L, r.getAccountId());
        assertFalse(r.isCreated());
    }

    @Test
    void 账户匹配_未命中则创建并批内去重() {
        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(account(3L, "现金", "asset")));
        List<Account> created = new ArrayList<>();
        BillAccountMatcher m = matcher();
        BillAccountMatcher.MatchResult r1 = m.match(1L, "余额宝（含余额）", created);
        assertTrue(r1.isCreated());
        // 第二次同方式：批内命中，不重复创建
        BillAccountMatcher.MatchResult r2 = m.match(1L, "余额宝（含余额）", created);
        assertTrue(r2.isCreated());
        assertEquals(r1.getAccountId(), r2.getAccountId());
        assertEquals(1, created.size());
    }

    @Test
    void 未知收付款方式归现金() {
        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(account(3L, "现金", "asset")));
        BillAccountMatcher m = matcher();
        BillAccountMatcher.MatchResult r = m.match(1L, "", new ArrayList<>());
        assertEquals(3L, r.getAccountId());
        assertFalse(r.isCreated());
    }

    @Test
    void 分类映射_命中映射表() {
        assertEquals("餐饮", BillCategoryMapper.map("餐饮美食"));
        assertEquals("交通", BillCategoryMapper.map("交通出行"));
        assertEquals("工资", BillCategoryMapper.map("工资"));
    }

    @Test
    void 分类映射_未命中返回null由未分类兜底() {
        assertNull(BillCategoryMapper.map("奇怪的分类"));
    }

    @Test
    void 行转DTO_方向与单号() {
        BillCsvParser.Row row = new BillCsvParser.Row();
        row.setDate("2026-08-03");
        row.setTime("12:30");
        row.setDescription("美团-午餐");
        row.setAmount(new BigDecimal("32.00"));
        row.setDirection("expense");
        row.setOrderNo("2026080322001488x");
        TransactionDTO dto = ImportService.buildTransactionDTO(row, 10L, 4L, 55L);
        assertEquals("expense", dto.getTransactionType());
        assertEquals(new BigDecimal("32.00"), dto.getAmount());
        assertEquals(10L, dto.getCategoryId());
        assertEquals(4L, dto.getAccountId());
        assertEquals("2026080322001488x", dto.getExternalOrderNo());
        assertEquals(55L, dto.getImportBatchId());
    }

    @Test
    void 判重_已有单号() {
        when(transactionMapper.countByUserAndOrderNo(eq(1L), eq("2026080322001488x"))).thenReturn(1L);
        when(transactionMapper.countByUserAndOrderNo(eq(1L), eq("new-order"))).thenReturn(0L);
        assertTrue(ImportService.isDuplicate(transactionMapper, 1L, "2026080322001488x"));
        assertFalse(ImportService.isDuplicate(transactionMapper, 1L, "new-order"));
    }

    @Test
    void confirm_一行失败整批中止() {
        String csv = String.join("\n",
                "支付宝交易明细查询",
                "账号:[1111@qq.com]",
                "起始日期:[2026-08-01]  终止日期:[2026-08-03]",
                "---------------------------------交易记录明细列表------------------------------------",
                "交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注",
                "2026-08-03 12:30:00,餐饮美食,美团,美团-午餐,支出,32.00,招商银行(1234),交易成功,2026080322001488x,M001,",
                "2026-08-02 09:00:00,交通出行,滴滴,滴滴打车,支出,45.50,招商银行(1234),交易成功,2026080222001489x,M002,");

        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(
                account(3L, "现金", "asset"),
                account(4L, "招商银行(1234)", "asset")));
        when(categoryMapper.selectByUserIdAndType(1L, "expense")).thenReturn(java.util.Arrays.asList(
                category(10L, "餐饮", "expense"),
                category(11L, "交通", "expense")));
        when(transactionMapper.countByUserAndOrderNo(eq(1L), any())).thenReturn(0L);
        when(transactionService.createTransaction(eq(1L), any(TransactionDTO.class)))
                .thenThrow(new BusinessException("模拟校验失败"));

        ImportService service = new ImportService(accountMapper, categoryMapper,
                transactionMapper, transactionService, matcher(), importBatchMapper);

        ImportConfirmDTO dto = new ImportConfirmDTO();
        dto.setFileName("alipay.csv");
        dto.setContentBase64(Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)));

        assertThrows(BusinessException.class, () -> service.confirm(1L, dto));
        // 第一行失败即中止，第二行不再落账
        verify(transactionService, times(1)).createTransaction(anyLong(), any(TransactionDTO.class));
    }

    private static String alipayCsv(String category, String direction, String description, String orderNo) {
        return String.join("\n",
                "支付宝交易明细查询",
                "账号:[1111@qq.com]",
                "起始日期:[2026-08-01]  终止日期:[2026-08-03]",
                "---------------------------------交易记录明细列表------------------------------------",
                "交易时间,交易分类,对方名称,商品说明,收/支,金额,收/付款方式,当前状态,交易单号,商家订单号,备注",
                "2026-08-03 12:30:00," + category + ",美团," + description + "," + direction
                        + ",32.00,现金,交易成功," + orderNo + ",M001,");
    }

    @Test
    void confirm_未知分类无兜底时创建未分类() {
        String csv = alipayCsv("奇怪的分类", "支出", "美团-午餐", "ORD-UNK-1");
        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(account(3L, "现金", "asset")));
        when(categoryMapper.selectByUserIdAndType(1L, "expense")).thenReturn(new ArrayList<>());
        when(transactionMapper.countByUserAndOrderNo(eq(1L), any())).thenReturn(0L);
        when(importBatchMapper.insert(any(ImportBatch.class))).thenAnswer(inv -> {
            ImportBatch batch = inv.getArgument(0);
            batch.setId(55L);
            return 1;
        });
        when(categoryMapper.insert(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(99L);
            return 1;
        });

        ImportService service = new ImportService(accountMapper, categoryMapper,
                transactionMapper, transactionService, matcher(), importBatchMapper);
        ImportConfirmDTO dto = new ImportConfirmDTO();
        dto.setFileName("alipay.csv");
        dto.setContentBase64(Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)));

        ImportConfirmVO vo = service.confirm(1L, dto);
        assertEquals(1, vo.getImportedCount());
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(categoryCaptor.capture());
        assertEquals("未分类", categoryCaptor.getValue().getName());
        ArgumentCaptor<TransactionDTO> dtoCaptor = ArgumentCaptor.forClass(TransactionDTO.class);
        verify(transactionService).createTransaction(eq(1L), dtoCaptor.capture());
        assertEquals(99L, dtoCaptor.getValue().getCategoryId());
    }

    @Test
    void confirm_收入未知分类兜底其他收入() {
        String csv = alipayCsv("奇怪的分类", "收入", "退款", "ORD-UNK-2");
        when(accountMapper.selectByUserId(1L)).thenReturn(accounts(account(3L, "现金", "asset")));
        when(categoryMapper.selectByUserIdAndType(1L, "income")).thenReturn(
                List.of(category(21L, "其他收入", "income")));
        when(transactionMapper.countByUserAndOrderNo(eq(1L), any())).thenReturn(0L);
        when(importBatchMapper.insert(any(ImportBatch.class))).thenAnswer(inv -> {
            ImportBatch batch = inv.getArgument(0);
            batch.setId(56L);
            return 1;
        });

        ImportService service = new ImportService(accountMapper, categoryMapper,
                transactionMapper, transactionService, matcher(), importBatchMapper);
        ImportConfirmDTO dto = new ImportConfirmDTO();
        dto.setFileName("alipay.csv");
        dto.setContentBase64(Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)));

        ImportConfirmVO vo = service.confirm(1L, dto);
        assertEquals(1, vo.getImportedCount());
        verify(categoryMapper, never()).insert(any(Category.class));
        ArgumentCaptor<TransactionDTO> dtoCaptor = ArgumentCaptor.forClass(TransactionDTO.class);
        verify(transactionService).createTransaction(eq(1L), dtoCaptor.capture());
        assertEquals(21L, dtoCaptor.getValue().getCategoryId());
    }
}
