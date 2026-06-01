package com.duoduocode.service.dashboard.service;

import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.dashboard.dto.DashboardVO;
import com.duoduocode.service.dashboard.mapper.DashboardMapper;
import com.duoduocode.service.account.entity.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final AccountMapper accountMapper;

    /**
     * 获取首页数据看板
     *
     * @param userId 用户ID
     * @return 看板数据
     */
    @Transactional(readOnly = true)
    public DashboardVO getDashboard(Long userId) {
        DashboardVO vo = new DashboardVO();

        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 本月第一天和最后一天
        YearMonth yearMonth = YearMonth.from(today);
        String monthStart = yearMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String monthEnd = yearMonth.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 上月
        YearMonth lastMonth = yearMonth.minusMonths(1);
        String lastMonthStart = lastMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastMonthEnd = lastMonth.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 本周一
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        String weekStartStr = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 1. 账户汇总
        List<Account> accounts = accountMapper.selectByUserId(userId);
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalInvestments = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
            switch (account.getType()) {
                case "asset":
                    totalAssets = totalAssets.add(balance);
                    break;
                case "liability":
                    totalLiabilities = totalLiabilities.add(balance);
                    break;
                case "investment":
                    totalInvestments = totalInvestments.add(balance);
                    break;
            }
        }

        vo.setTotalAssets(totalAssets);
        vo.setTotalLiabilities(totalLiabilities);
        vo.setTotalInvestments(totalInvestments);
        vo.setNetWorth(totalAssets.add(totalInvestments).subtract(totalLiabilities));
        vo.setAccountCount(accounts.size());

        // 2. 上月净资产
        BigDecimal lastMonthNetWorth = dashboardMapper.getLastMonthNetWorth(userId, monthStart);
        if (lastMonthNetWorth == null) {
            lastMonthNetWorth = BigDecimal.ZERO;
        }
        vo.setLastMonthNetWorth(lastMonthNetWorth);

        // 3. 净资产变化
        BigDecimal netWorthChange = vo.getNetWorth().subtract(lastMonthNetWorth);
        vo.setNetWorthChange(netWorthChange);

        if (lastMonthNetWorth.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal percent = netWorthChange.divide(lastMonthNetWorth, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setNetWorthChangePercent(percent);
        } else {
            vo.setNetWorthChangePercent(BigDecimal.ZERO);
        }

        // 4. 月度收支汇总
        Map<String, Object> monthlySummary = dashboardMapper.getMonthlySummary(userId, monthStart, monthEnd);
        if (monthlySummary != null) {
            BigDecimal totalIncome = monthlySummary.get("totalIncome") != null
                    ? new BigDecimal(monthlySummary.get("totalIncome").toString()) : BigDecimal.ZERO;
            BigDecimal totalExpense = monthlySummary.get("totalExpense") != null
                    ? new BigDecimal(monthlySummary.get("totalExpense").toString()) : BigDecimal.ZERO;

            vo.setTotalIncome(totalIncome);
            vo.setTotalExpense(totalExpense);
            vo.setNetAmount(totalIncome.subtract(totalExpense));
            vo.setIncomeCount(monthlySummary.get("incomeCount") != null
                    ? ((Number) monthlySummary.get("incomeCount")).intValue() : 0);
            vo.setExpenseCount(monthlySummary.get("expenseCount") != null
                    ? ((Number) monthlySummary.get("expenseCount")).intValue() : 0);
            vo.setMonthTransactionCount(vo.getIncomeCount() + vo.getExpenseCount());
        } else {
            vo.setTotalIncome(BigDecimal.ZERO);
            vo.setTotalExpense(BigDecimal.ZERO);
            vo.setNetAmount(BigDecimal.ZERO);
            vo.setIncomeCount(0);
            vo.setExpenseCount(0);
            vo.setMonthTransactionCount(0);
        }

        // 5. 支出TOP5分类
        List<Map<String, Object>> topCategories = dashboardMapper.getTopExpenseCategories(
                userId, monthStart, monthEnd, 5);
        List<DashboardVO.CategoryExpenseVO> topExpenseCategories = new ArrayList<>();

        BigDecimal totalExpenseAmt = vo.getTotalExpense();
        for (Map<String, Object> category : topCategories) {
            DashboardVO.CategoryExpenseVO catVO = new DashboardVO.CategoryExpenseVO();
            catVO.setCategoryId(((Number) category.get("categoryId")).longValue());
            catVO.setCategoryName((String) category.get("categoryName"));
            catVO.setCategoryIcon((String) category.get("categoryIcon"));
            catVO.setAmount(new BigDecimal(category.get("totalExpense").toString()));
            catVO.setTransactionCount(((Number) category.get("txCount")).intValue());

            if (totalExpenseAmt.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal percent = catVO.getAmount()
                        .divide(totalExpenseAmt, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                catVO.setPercentage(percent);
            } else {
                catVO.setPercentage(BigDecimal.ZERO);
            }

            topExpenseCategories.add(catVO);
        }
        vo.setTopExpenseCategories(topExpenseCategories);

        // 6. 今日/本周支出
        BigDecimal todayExpense = dashboardMapper.getExpenseByDateRange(userId, todayStr, todayStr);
        BigDecimal weekExpense = dashboardMapper.getExpenseByDateRange(userId, weekStartStr, todayStr);

        vo.setTodayExpense(todayExpense != null ? todayExpense : BigDecimal.ZERO);
        vo.setWeekExpense(weekExpense != null ? weekExpense : BigDecimal.ZERO);

        return vo;
    }

    /**
     * 获取净资产趋势
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 净资产数据
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNetWorthTrend(Long userId, String startDate, String endDate) {
        // 获取起始净资产
        BigDecimal startNetWorth = dashboardMapper.getLastMonthNetWorth(userId, startDate);
        if (startNetWorth == null) {
            startNetWorth = BigDecimal.ZERO;
        }

        // 获取当前净资产
        List<Account> accounts = accountMapper.selectByUserId(userId);
        BigDecimal currentNetWorth = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (Boolean.TRUE.equals(account.getIncludeInNetWorth())) {
                BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
                if ("asset".equals(account.getType()) || "investment".equals(account.getType())) {
                    currentNetWorth = currentNetWorth.add(balance);
                } else if ("liability".equals(account.getType())) {
                    currentNetWorth = currentNetWorth.subtract(balance);
                }
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("startNetWorth", startNetWorth);
        result.put("currentNetWorth", currentNetWorth);
        result.put("change", currentNetWorth.subtract(startNetWorth));

        return result;
    }

    /**
     * 获取月度收支详情
     *
     * @param userId 用户ID
     * @param month  月份 (YYYY-MM)
     * @return 月度收支数据
     */
    @Transactional(readOnly = true)
    public DashboardVO getMonthlyDetail(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        String monthStart = yearMonth.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String monthEnd = yearMonth.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);

        DashboardVO vo = new DashboardVO();

        // 获取月度收支汇总
        Map<String, Object> summary = dashboardMapper.getMonthlySummary(userId, monthStart, monthEnd);
        if (summary != null) {
            vo.setTotalIncome(summary.get("totalIncome") != null
                    ? new BigDecimal(summary.get("totalIncome").toString()) : BigDecimal.ZERO);
            vo.setTotalExpense(summary.get("totalExpense") != null
                    ? new BigDecimal(summary.get("totalExpense").toString()) : BigDecimal.ZERO);
            vo.setNetAmount(vo.getTotalIncome().subtract(vo.getTotalExpense()));
            vo.setIncomeCount(((Number) summary.get("incomeCount")).intValue());
            vo.setExpenseCount(((Number) summary.get("expenseCount")).intValue());
            vo.setMonthTransactionCount(vo.getIncomeCount() + vo.getExpenseCount());
        }

        // 获取支出TOP5
        List<Map<String, Object>> topCategories = dashboardMapper.getTopExpenseCategories(
                userId, monthStart, monthEnd, 5);
        List<DashboardVO.CategoryExpenseVO> topExpenseCategories = new ArrayList<>();

        for (Map<String, Object> category : topCategories) {
            DashboardVO.CategoryExpenseVO catVO = new DashboardVO.CategoryExpenseVO();
            catVO.setCategoryId(((Number) category.get("categoryId")).longValue());
            catVO.setCategoryName((String) category.get("categoryName"));
            catVO.setCategoryIcon((String) category.get("categoryIcon"));
            catVO.setAmount(new BigDecimal(category.get("totalExpense").toString()));
            catVO.setTransactionCount(((Number) category.get("txCount")).intValue());
            topExpenseCategories.add(catVO);
        }
        vo.setTopExpenseCategories(topExpenseCategories);

        return vo;
    }
}
