package com.duoduocode.service.report.service;

import com.duoduocode.service.report.dto.*;
import com.duoduocode.service.report.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportMapper reportMapper;

    /**
     * 获取收支报表
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 收支报表数据
     */
    public IncomeExpenseReportVO getIncomeExpenseReport(Long userId, String startDate, String endDate) {
        IncomeExpenseReportVO report = new IncomeExpenseReportVO();
        
        // 查询月度收支数据
        List<MonthlyData> monthlyDataList = reportMapper.selectMonthlyIncomeExpense(userId, startDate, endDate);
        report.setMonthlyData(monthlyDataList);
        
        // 计算总收入和总支出
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (MonthlyData data : monthlyDataList) {
            if (data.getIncome() != null) {
                totalIncome = totalIncome.add(data.getIncome());
            }
            if (data.getExpense() != null) {
                totalExpense = totalExpense.add(data.getExpense());
            }
        }
        
        report.setTotalIncome(totalIncome);
        report.setTotalExpense(totalExpense);
        report.setBalance(totalIncome.subtract(totalExpense));
        
        // 查询支出分类排行
        List<CategoryData> topExpenseCategories = reportMapper.selectCategoryStats(userId, "expense", startDate, endDate);
        report.setTopExpenseCategories(topExpenseCategories);
        
        return report;
    }

    /**
     * 获取账户趋势报表
     * @param userId 用户ID
     * @param accountId 账户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 账户趋势数据
     */
    public AccountTrendReportVO getAccountTrendReport(Long userId, Long accountId, String startDate, String endDate) {
        AccountTrendReportVO report = new AccountTrendReportVO();
        
        report.setAccountId(accountId);
        // TODO: 从账户服务获取账户名称
        report.setAccountName("");
        
        // 查询账户每日余额
        List<DailyBalance> dailyBalances = reportMapper.selectAccountDailyBalance(accountId, startDate, endDate);
        report.setDailyBalances(dailyBalances);
        
        // 计算最大和最小余额
        BigDecimal maxBalance = null;
        BigDecimal minBalance = null;
        
        for (DailyBalance balance : dailyBalances) {
            if (balance.getBalance() != null) {
                if (maxBalance == null || balance.getBalance().compareTo(maxBalance) > 0) {
                    maxBalance = balance.getBalance();
                }
                if (minBalance == null || balance.getBalance().compareTo(minBalance) < 0) {
                    minBalance = balance.getBalance();
                }
            }
        }
        
        report.setMaxBalance(maxBalance != null ? maxBalance : BigDecimal.ZERO);
        report.setMinBalance(minBalance != null ? minBalance : BigDecimal.ZERO);
        
        return report;
    }

    /**
     * 获取分类分析报表
     * @param userId 用户ID
     * @param type 类型(income/expense)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类分析数据列表
     */
    public List<CategoryAnalysisReportVO> getCategoryAnalysisReport(Long userId, String type, String startDate, String endDate) {
        List<CategoryData> categoryDataList = reportMapper.selectCategoryStats(userId, type, startDate, endDate);
        
        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CategoryData data : categoryDataList) {
            if (data.getAmount() != null) {
                totalAmount = totalAmount.add(data.getAmount());
            }
        }
        
        // 转换为VO并计算百分比
        List<CategoryAnalysisReportVO> result = new java.util.ArrayList<>();
        for (CategoryData data : categoryDataList) {
            CategoryAnalysisReportVO vo = new CategoryAnalysisReportVO();
            vo.setCategoryId(data.getCategoryId());
            vo.setCategoryName(data.getCategoryName());
            vo.setAmount(data.getAmount());
            vo.setTransactionCount(data.getTransactionCount());
            
            // 计算百分比
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0 && data.getAmount() != null) {
                Double percentage = data.getAmount()
                        .multiply(new BigDecimal("100"))
                        .divide(totalAmount, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                vo.setPercentage(percentage);
            } else {
                vo.setPercentage(0.0);
            }
            
            result.add(vo);
        }
        
        return result;
    }

    /**
     * 获取月度对比报表
     * @param userId 用户ID
     * @param month1 第一个月份
     * @param month2 第二个月份
     * @return 月度对比数据
     */
    public MonthlyComparisonVO getMonthlyComparisonReport(Long userId, String month1, String month2) {
        MonthlyComparisonVO report = new MonthlyComparisonVO();
        report.setMonth1(month1);
        report.setMonth2(month2);
        
        // 计算月份的起止日期
        String startDate1 = month1 + "-01";
        String startDate2 = month2 + "-01";
        String endDate1 = getMonthEndDate(month1);
        String endDate2 = getMonthEndDate(month2);
        
        // 查询两个月份的收支数据
        List<MonthlyData> dataList1 = reportMapper.selectMonthlyIncomeExpense(userId, startDate1, endDate1);
        List<MonthlyData> dataList2 = reportMapper.selectMonthlyIncomeExpense(userId, startDate2, endDate2);
        
        // 计算月份1的数据
        BigDecimal income1 = BigDecimal.ZERO;
        BigDecimal expense1 = BigDecimal.ZERO;
        for (MonthlyData data : dataList1) {
            if (data.getIncome() != null) {
                income1 = income1.add(data.getIncome());
            }
            if (data.getExpense() != null) {
                expense1 = expense1.add(data.getExpense());
            }
        }
        
        // 计算月份2的数据
        BigDecimal income2 = BigDecimal.ZERO;
        BigDecimal expense2 = BigDecimal.ZERO;
        for (MonthlyData data : dataList2) {
            if (data.getIncome() != null) {
                income2 = income2.add(data.getIncome());
            }
            if (data.getExpense() != null) {
                expense2 = expense2.add(data.getExpense());
            }
        }
        
        report.setIncome1(income1);
        report.setIncome2(income2);
        report.setExpense1(expense1);
        report.setExpense2(expense2);
        
        // 计算变化值
        report.setIncomeChange(income2.subtract(income1));
        report.setExpenseChange(expense2.subtract(expense1));
        
        return report;
    }
    
    /**
     * 获取月份的最后一天
     */
    private String getMonthEndDate(String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);
        
        int lastDay;
        switch (mon) {
            case 2:
                // 闰年判断
                lastDay = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0) ? 29 : 28;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                lastDay = 30;
                break;
            default:
                lastDay = 31;
        }
        
        return String.format("%s-%02d", month, lastDay);
    }
}
