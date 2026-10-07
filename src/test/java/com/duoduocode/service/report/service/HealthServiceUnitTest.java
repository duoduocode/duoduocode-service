package com.duoduocode.service.report.service;

import com.duoduocode.service.account.mapper.AccountMapper;
import com.duoduocode.service.report.dto.HealthVO;
import com.duoduocode.service.report.mapper.ReportMapper;
import com.duoduocode.service.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * R004 U409–U410：健康度三指标计算/区间判定矩阵与四类降级矩阵（纯函数驱动）。
 * 阈值（S2-Q2 已裁决）：负债率 ≤30 优/(30,50] 良/>50 警戒；流动比率 ≥2 优/[1,2) 良/<1 警戒；
 * 应急金 ≥6 优/[3,6) 良/<3 警戒。
 */
class HealthServiceUnitTest {

    private final HealthService service = new HealthService(
            mock(AccountMapper.class), mock(ReportMapper.class), mock(UserMapper.class));

    private HealthVO health(String totalAssets, String totalLiabilities,
                            String cashAssets, String avgMonthlyExpense, int completeMonths) {
        return service.buildHealth(
                totalAssets == null ? null : new BigDecimal(totalAssets),
                totalLiabilities == null ? null : new BigDecimal(totalLiabilities),
                cashAssets == null ? null : new BigDecimal(cashAssets),
                avgMonthlyExpense == null ? null : new BigDecimal(avgMonthlyExpense),
                completeMonths);
    }

    private void assertDecimal(String want, BigDecimal actual) {
        assertTrue(actual != null && new BigDecimal(want).compareTo(actual) == 0,
                "期望 " + want + " 实际 " + actual);
    }

    /** 正常场景：总资产 200000 / 总负债 60000 / 现金 18000 / 月均支出 3000（与 I426 同口径）。 */
    private HealthVO normal() {
        return health("200000", "60000", "18000", "3000", 6);
    }

    @Test
    void U409_三指标_计算与展示格式() {
        HealthVO vo = normal();
        assertDecimal("0.30", vo.getDebtRatio().getValue());
        assertEquals("excellent", vo.getDebtRatio().getLevel());
        assertEquals("30%", vo.getDebtRatio().getDisplay());
        assertNotNull(vo.getDebtRatio().getHint(), "静态提示不应为空");
        assertNull(vo.getDebtRatio().getDegradedReason());

        assertDecimal("0.30", vo.getCurrentRatio().getValue());
        assertEquals("warning", vo.getCurrentRatio().getLevel());
        assertEquals("0.30", vo.getCurrentRatio().getDisplay());

        assertDecimal("6.0", vo.getEmergencyMonths().getValue());
        assertEquals("excellent", vo.getEmergencyMonths().getLevel());
        assertEquals("6.0月", vo.getEmergencyMonths().getDisplay());
    }

    @Test
    void U409_负债率区间边界_30_31_50_51() {
        assertDecimal("0.30", health("200000", "60000", null, null, 0).getDebtRatio().getValue());
        assertEquals("excellent", health("200000", "60000", null, null, 0).getDebtRatio().getLevel());
        assertEquals("good", health("200000", "62000", null, null, 0).getDebtRatio().getLevel());
        assertEquals("good", health("200000", "100000", null, null, 0).getDebtRatio().getLevel());
        assertEquals("warning", health("200000", "102000", null, null, 0).getDebtRatio().getLevel());
    }

    @Test
    void U409_流动比率区间边界_2_199_100_099() {
        assertEquals("excellent", health(null, "10000", "20000", null, 0).getCurrentRatio().getLevel());
        assertEquals("good", health(null, "10000", "19900", null, 0).getCurrentRatio().getLevel());
        assertEquals("good", health(null, "10000", "10000", null, 0).getCurrentRatio().getLevel());
        assertEquals("warning", health(null, "10000", "9900", null, 0).getCurrentRatio().getLevel());
    }

    @Test
    void U409_应急金区间边界_6_59_30_29() {
        assertEquals("excellent", health(null, null, "18000", "3000", 6).getEmergencyMonths().getLevel());
        assertEquals("good", health(null, null, "17700", "3000", 6).getEmergencyMonths().getLevel());
        assertEquals("good", health(null, null, "9000", "3000", 6).getEmergencyMonths().getLevel());
        assertEquals("warning", health(null, null, "8700", "3000", 6).getEmergencyMonths().getLevel());
    }

    @Test
    void U410_降级_no_data_总资产为零() {
        HealthVO vo = health("0", "5000", "0", "3000", 6);
        assertNull(vo.getDebtRatio().getValue(), "总资产 0 负债率应降级");
        assertEquals("—", vo.getDebtRatio().getDisplay());
        assertEquals("no_data", vo.getDebtRatio().getDegradedReason());
        // 流动比率与应急金不受影响照常计算
        assertDecimal("0.00", vo.getCurrentRatio().getValue());
        assertDecimal("0.0", vo.getEmergencyMonths().getValue());
    }

    @Test
    void U410_降级_no_liability_无负债() {
        HealthVO vo = health("200000", "0", "18000", "3000", 6);
        assertDecimal("0.00", vo.getDebtRatio().getValue());
        assertEquals("excellent", vo.getDebtRatio().getLevel());
        assertNull(vo.getCurrentRatio().getValue(), "无负债流动比率应降级");
        assertEquals("—", vo.getCurrentRatio().getDisplay());
        assertEquals("no_liability", vo.getCurrentRatio().getDegradedReason());
        assertDecimal("6.0", vo.getEmergencyMonths().getValue());
    }

    @Test
    void U410_降级_insufficient_history_不足一个完整月() {
        HealthVO vo = health("200000", "60000", "18000", "3000", 0);
        assertNull(vo.getEmergencyMonths().getValue());
        assertEquals("—", vo.getEmergencyMonths().getDisplay());
        assertEquals("insufficient_history", vo.getEmergencyMonths().getDegradedReason());
        // 其余两指标照常
        assertDecimal("0.30", vo.getDebtRatio().getValue());
        assertDecimal("0.30", vo.getCurrentRatio().getValue());
    }

    @Test
    void U410_降级_no_expense_有完整月但月均支出为零() {
        HealthVO vo = health("200000", "60000", "18000", "0", 6);
        assertNull(vo.getEmergencyMonths().getValue(), "月均支出 0 不应得到无穷大");
        assertEquals("—", vo.getEmergencyMonths().getDisplay());
        assertEquals("no_expense", vo.getEmergencyMonths().getDegradedReason());
    }
}
