package com.duoduocode.service.investment.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.investment.dto.InvestmentValueDTO;
import com.duoduocode.service.investment.dto.InvestmentValueVO;
import com.duoduocode.service.investment.entity.InvestmentValue;
import com.duoduocode.service.investment.mapper.InvestmentValueMapper;
import com.duoduocode.service.common.BusinessException;
import com.duoduocode.service.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 投资市值服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentValueService {

    private final InvestmentValueMapper investmentValueMapper;
    private final AccountMapper accountMapper;

    /**
     * 记录市值
     *
     * @param accountId 账户ID
     * @param dto 市值数据
     * @return 创建的记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long recordMarketValue(Long accountId, InvestmentValueDTO dto) {
        // 验证账户存在且为投资类型
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        if (!"investment".equals(account.getType())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只有投资账户才能记录市值");
        }

        // 验证必填参数
        if (dto.getDate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "记录日期不能为空");
        }
        if (dto.getMarketValue() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "市值不能为空");
        }

        // 检查是否已存在该日期的记录
        InvestmentValue existing = investmentValueMapper.selectByAccountIdAndDate(
                accountId, dto.getDate().toString());
        if (existing != null) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该日期已存在市值记录");
        }

        // 创建市值记录
        InvestmentValue value = new InvestmentValue();
        value.setAccountId(accountId);
        value.setDate(dto.getDate());
        value.setMarketValue(dto.getMarketValue());
        value.setCostBasis(dto.getCostBasis() != null ? dto.getCostBasis() : BigDecimal.ZERO);
        value.setNote(dto.getNote());
        value.setCreatedAt(LocalDateTime.now());
        value.setUpdatedAt(LocalDateTime.now());

        investmentValueMapper.insert(value);
        return value.getId();
    }

    /**
     * 获取市值历史
     *
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 市值历史列表
     */
    public List<InvestmentValueVO> getMarketValueHistory(Long accountId, String startDate, String endDate) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        List<InvestmentValue> list = investmentValueMapper.selectByAccountIdAndDateRange(
                accountId, startDate, endDate);

        List<InvestmentValueVO> result = new ArrayList<>();
        for (InvestmentValue value : list) {
            result.add(convertToVO(value));
        }
        return result;
    }

    /**
     * 获取最新市值
     *
     * @param accountId 账户ID
     * @return 最新市值记录
     */
    public InvestmentValueVO getLatestMarketValue(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }

        InvestmentValue value = investmentValueMapper.selectLatestByAccountId(accountId);
        if (value == null) {
            return null;
        }
        return convertToVO(value);
    }

    /**
     * 计算收益
     *
     * @param accountId 账户ID
     * @return 收益信息
     */
    public Map<String, Object> calculateProfit(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "账户不存在");
        }
        if (!"investment".equals(account.getType())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只有投资账户才能计算收益");
        }

        Map<String, Object> result = new HashMap<>();

        // 获取最新市值记录
        InvestmentValue latest = investmentValueMapper.selectLatestByAccountId(accountId);
        if (latest != null) {
            result.put("latestMarketValue", latest.getMarketValue());
            result.put("latestCostBasis", latest.getCostBasis());
            BigDecimal latestProfit = latest.getMarketValue().subtract(latest.getCostBasis());
            result.put("latestProfit", latestProfit);
            BigDecimal latestProfitRate = calculateProfitRate(latest.getMarketValue(), latest.getCostBasis());
            result.put("latestProfitRate", latestProfitRate);
        } else {
            result.put("latestMarketValue", BigDecimal.ZERO);
            result.put("latestCostBasis", BigDecimal.ZERO);
            result.put("latestProfit", BigDecimal.ZERO);
            result.put("latestProfitRate", BigDecimal.ZERO);
        }

        // 计算总收益（根据所有记录）
        BigDecimal totalProfit = investmentValueMapper.calculateTotalProfit(accountId);
        result.put("totalProfit", totalProfit != null ? totalProfit : BigDecimal.ZERO);

        return result;
    }

    // ===== 私有辅助方法 =====

    /**
     * 将实体转换为VO
     */
    private InvestmentValueVO convertToVO(InvestmentValue value) {
        InvestmentValueVO vo = new InvestmentValueVO();
        BeanUtils.copyProperties(value, vo);
        return vo;
    }

    /**
     * 计算收益率
     */
    private BigDecimal calculateProfitRate(BigDecimal marketValue, BigDecimal costBasis) {
        if (costBasis == null || costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return marketValue.subtract(costBasis)
                .divide(costBasis, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}