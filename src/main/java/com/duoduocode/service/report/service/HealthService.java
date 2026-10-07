package com.duoduocode.service.report.service;

import com.duoduocode.service.account.entity.Account;
import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.entity.User;
import com.duoduocode.service.report.dto.HealthVO;
import com.duoduocode.service.report.dto.MonthlyData;
import com.duoduocode.service.report.mapper.ReportMapper;
import com.duoduocode.service.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 资产负债健康度服务（R004）
 * 三指标：负债率（dashboard 聚合口径，含投资）/ 流动比率（仅 asset 现金）/ 应急金月数（近 6 个完整月均值）
 * 阈值（S2-Q2）：负债率 ≤30 优/(30,50] 良/>50 警戒；流动比率 ≥2 优/[1,2) 良/<1 警戒；应急金 ≥6 优/[3,6) 良/<3 警戒
 */
@Slf4j
@Service
public class HealthService {

    /**
     * 应急金均值窗口：近 6 个完整月
     */
    private static final int EMERGENCY_WINDOW_MONTHS = 6;

    private final AccountMapper accountMapper;
    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    public HealthService(AccountMapper accountMapper, ReportMapper reportMapper, UserMapper userMapper) {
        this.accountMapper = accountMapper;
        this.reportMapper = reportMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询用户健康度三指标（实时计算，无快照）
     */
    public HealthVO getHealth(Long userId) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal cashAssets = BigDecimal.ZERO;
        List<Account> accounts = accountMapper.selectByUserId(userId);
        if (accounts != null) {
            for (Account account : accounts) {
                BigDecimal balance = account.getCurrentBalance() == null
                        ? BigDecimal.ZERO : account.getCurrentBalance();
                if ("asset".equals(account.getType())) {
                    totalAssets = totalAssets.add(balance);
                    cashAssets = cashAssets.add(balance);
                } else if ("investment".equals(account.getType())) {
                    totalAssets = totalAssets.add(balance);
                } else if ("liability".equals(account.getType())) {
                    totalLiabilities = totalLiabilities.add(balance);
                }
            }
        }

        User user = userMapper.selectById(userId);
        int completeMonths = countCompleteMonths(user == null ? null : user.getCreatedAt());
        BigDecimal avgMonthlyExpense = null;
        if (completeMonths > 0) {
            avgMonthlyExpense = averageMonthlyExpense(userId, completeMonths);
        }

        return buildHealth(totalAssets, totalLiabilities, cashAssets, avgMonthlyExpense, completeMonths);
    }

    /**
     * 三指标纯函数组装（U409/U410 单测直接驱动）
     *
     * @param totalAssets       总资产（asset + investment 余额合计，dashboard 口径）
     * @param totalLiabilities  总负债（liability 余额合计，负数或 0）
     * @param cashAssets        现金类资产（仅 asset 余额合计）
     * @param avgMonthlyExpense 近 6 个完整月月均支出（无完整月时 null）
     * @param completeMonths    注册后可用完整月数
     */
    public HealthVO buildHealth(BigDecimal totalAssets, BigDecimal totalLiabilities,
                                BigDecimal cashAssets, BigDecimal avgMonthlyExpense, int completeMonths) {
        HealthVO vo = new HealthVO();
        vo.setDebtRatio(buildDebtRatio(nz(totalAssets), nz(totalLiabilities)));
        vo.setCurrentRatio(buildCurrentRatio(nz(totalLiabilities), nz(cashAssets)));
        vo.setEmergencyMonths(buildEmergencyMonths(nz(cashAssets), avgMonthlyExpense, completeMonths));
        return vo;
    }

    // ===== 指标构造 =====

    /**
     * 负债率 = |总负债| ÷ 总资产；总资产为 0 → 降级 no_data
     */
    private HealthVO.Metric buildDebtRatio(BigDecimal totalAssets, BigDecimal totalLiabilities) {
        if (totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return degraded("no_data", "暂无资产数据，记账后即可评估负债状况");
        }
        BigDecimal ratio = totalLiabilities.abs()
                .divide(totalAssets, 2, RoundingMode.HALF_UP);
        HealthVO.Metric m = new HealthVO.Metric();
        m.setValue(ratio);
        m.setLevel(ratio.compareTo(new BigDecimal("0.30")) <= 0 ? "excellent"
                : ratio.compareTo(new BigDecimal("0.50")) <= 0 ? "good" : "warning");
        m.setDisplay(ratio.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).toPlainString() + "%");
        m.setHint("负债率 = 总负债 ÷ 总资产，30% 以内为优");
        return m;
    }

    /**
     * 流动比率 = 现金类资产 ÷ |总负债|；总负债为 0 → 降级 no_liability
     */
    private HealthVO.Metric buildCurrentRatio(BigDecimal totalLiabilities, BigDecimal cashAssets) {
        BigDecimal liabilities = totalLiabilities.abs();
        if (liabilities.compareTo(BigDecimal.ZERO) == 0) {
            return degraded("no_liability", "暂无负债，短期偿债无压力");
        }
        BigDecimal ratio = cashAssets.divide(liabilities, 2, RoundingMode.HALF_UP);
        HealthVO.Metric m = new HealthVO.Metric();
        m.setValue(ratio);
        m.setLevel(ratio.compareTo(new BigDecimal("2")) >= 0 ? "excellent"
                : ratio.compareTo(BigDecimal.ONE) >= 0 ? "good" : "warning");
        m.setDisplay(ratio.setScale(2, RoundingMode.HALF_UP).toPlainString());
        m.setHint("流动比率 = 现金类资产 ÷ 总负债，2 以上为优");
        return m;
    }

    /**
     * 应急金月数 = 现金类资产 ÷ 近 6 个完整月月均支出；
     * 无完整月 → insufficient_history；月均支出 0 → no_expense（不给无穷大）
     */
    private HealthVO.Metric buildEmergencyMonths(BigDecimal cashAssets, BigDecimal avgMonthlyExpense,
                                                 int completeMonths) {
        if (completeMonths <= 0) {
            return degraded("insufficient_history", "记账满一个完整月后可评估应急金");
        }
        if (avgMonthlyExpense == null || avgMonthlyExpense.compareTo(BigDecimal.ZERO) == 0) {
            return degraded("no_expense", "近几个月无支出记录，暂无法测算应急金");
        }
        BigDecimal months = cashAssets.divide(avgMonthlyExpense, 1, RoundingMode.HALF_UP);
        HealthVO.Metric m = new HealthVO.Metric();
        m.setValue(months);
        m.setLevel(months.compareTo(new BigDecimal("6")) >= 0 ? "excellent"
                : months.compareTo(new BigDecimal("3")) >= 0 ? "good" : "warning");
        m.setDisplay(months.setScale(1, RoundingMode.HALF_UP).toPlainString() + "月");
        m.setHint("应急金月数 = 现金类资产 ÷ 近6个月均支出，6 个月以上为优");
        return m;
    }

    private HealthVO.Metric degraded(String reason, String hint) {
        HealthVO.Metric m = new HealthVO.Metric();
        m.setDisplay("—");
        m.setHint(hint);
        m.setDegradedReason(reason);
        return m;
    }

    // ===== 数据聚合 =====

    /**
     * 注册后可用完整月数：首个完整自然月（注册次月；注册日为 1 日则当月）到当前月（不含）的月数
     */
    private int countCompleteMonths(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }
        YearMonth first = YearMonth.from(createdAt);
        if (createdAt.getDayOfMonth() > 1) {
            first = first.plusMonths(1);
        }
        YearMonth current = YearMonth.now();
        long months = first.until(current, java.time.temporal.ChronoUnit.MONTHS);
        return months < 0 ? 0 : (int) Math.min(months, Integer.MAX_VALUE);
    }

    /**
     * 近 min(6, completeMonths) 个完整月月均支出（按窗口月数平均，无交易月份计 0）
     */
    private BigDecimal averageMonthlyExpense(Long userId, int completeMonths) {
        int window = Math.min(EMERGENCY_WINDOW_MONTHS, completeMonths);
        YearMonth current = YearMonth.now();
        String startDate = current.minusMonths(window).atDay(1).toString();
        String endDate = current.minusMonths(1).atEndOfMonth().toString();

        BigDecimal sum = BigDecimal.ZERO;
        List<MonthlyData> data = reportMapper.selectMonthlyIncomeExpense(userId, startDate, endDate);
        if (data != null) {
            for (MonthlyData row : data) {
                if (row.getExpense() != null) {
                    sum = sum.add(row.getExpense());
                }
            }
        }
        return sum.divide(BigDecimal.valueOf(window), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
