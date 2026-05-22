package com.duoduocode.service.investment.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.investment.dto.InvestmentIncomeDTO;
import com.duoduocode.service.investment.dto.InvestmentIncomeVO;
import com.duoduocode.service.investment.entity.InvestmentIncome;
import com.duoduocode.service.investment.mapper.InvestmentIncomeMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 理财收益服务类 */
@Service
@RequiredArgsConstructor
public class InvestmentIncomeService {

    private final InvestmentIncomeMapper investmentIncomeMapper;
    private final AccountMapper accountMapper;

    /**
     * 记录收益
     *
     * @param accountId 账户ID
     * @param dto 收益数据
     * @return 创建的记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recordIncome(Long accountId, InvestmentIncomeDTO dto) {
        // 验证账户存在且为投资类型
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        if (!"investment".equals(account.getType())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只有投资账户才能记录收益");
        }

        // 验证必填参数
        if (dto.getIncomeDate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收益日期不能为空");
        }
        if (dto.getIncomeAmount() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收益金额不能为空");
        }
        if (dto.getIncomeType() == null || dto.getIncomeType().trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收益类型不能为空");
        }

        // 验证收益类型
        if (!isValidIncomeType(dto.getIncomeType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收益类型无效，必须是 interest、dividend、capital_gain 或 other");
        }

        // 创建收益记录
        InvestmentIncome income = new InvestmentIncome();
        income.setAccountId(accountId);
        income.setIncomeDate(dto.getIncomeDate());
        income.setIncomeAmount(dto.getIncomeAmount());
        income.setIncomeType(dto.getIncomeType());
        income.setDescription(dto.getDescription());
        income.setTransactionId(dto.getTransactionId());
        income.setCreatedAt(LocalDateTime.now());
        income.setUpdatedAt(LocalDateTime.now());

        investmentIncomeMapper.insert(income);
        return income.getId();
    }

    /**
     * 获取收益历史
     *
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收益历史列表
     */
    public List<InvestmentIncomeVO> getIncomeHistory(Long accountId, String startDate, String endDate) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        List<InvestmentIncome> list = investmentIncomeMapper.selectByAccountIdAndDateRange(
                accountId, startDate, endDate);

        List<InvestmentIncomeVO> result = new ArrayList<>();
        for (InvestmentIncome income : list) {
            result.add(convertToVO(income));
        }
        return result;
    }

    /**
     * 获取月度收益
     *
     * @param accountId 账户ID
     * @param month 月份 YYYY-MM
     * @return 月度收益统计
     */
    public Map<String, Object> getMonthlyIncome(Long accountId, String month) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        List<InvestmentIncome> list = investmentIncomeMapper.selectByAccountIdAndMonth(accountId, month);
        List<InvestmentIncomeVO> details = new ArrayList<>();
        for (InvestmentIncome income : list) {
            details.add(convertToVO(income));
        }

        // 计算月度总收入
        BigDecimal monthlyTotal = investmentIncomeMapper.calculateMonthlyIncome(accountId, month);

        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("totalIncome", monthlyTotal != null ? monthlyTotal : BigDecimal.ZERO);
        result.put("details", details);

        return result;
    }

    /**
     * 获取总收入
     *
     * @param accountId 账户ID
     * @return 总收益统计
     */
    public Map<String, Object> getTotalIncome(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        if (!"investment".equals(account.getType())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只有投资账户才能获取收益统计");
        }

        Map<String, Object> result = new HashMap<>();

        // 计算总收入
        BigDecimal totalIncome = investmentIncomeMapper.calculateTotalIncome(accountId);
        result.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);

        // 按类型统计收入
        Map<String, BigDecimal> incomeByType = new HashMap<>();
        String[] types = {"interest", "dividend", "capital_gain", "other"};
        for (String type : types) {
            BigDecimal amount = investmentIncomeMapper.calculateIncomeByType(accountId, type);
            incomeByType.put(type, amount != null ? amount : BigDecimal.ZERO);
        }
        result.put("incomeByType", incomeByType);

        // 获取所有收益记录
        List<InvestmentIncome> list = investmentIncomeMapper.selectByAccountId(accountId);
        List<InvestmentIncomeVO> details = new ArrayList<>();
        for (InvestmentIncome income : list) {
            details.add(convertToVO(income));
        }
        result.put("details", details);

        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 将实体转换为VO
     */
    private InvestmentIncomeVO convertToVO(InvestmentIncome income) {
        InvestmentIncomeVO vo = new InvestmentIncomeVO();
        BeanUtils.copyProperties(income, vo);
        // 触发类型名称设置
        vo.setIncomeType(income.getIncomeType());
        return vo;
    }

    /**
     * 验证收益类型是否有效
     */
    private boolean isValidIncomeType(String type) {
        return "interest".equals(type) ||
               "dividend".equals(type) ||
               "capital_gain".equals(type) ||
               "other".equals(type);
    }
}
