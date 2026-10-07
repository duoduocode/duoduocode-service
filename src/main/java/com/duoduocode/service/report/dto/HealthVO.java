package com.duoduocode.service.report.dto;

import java.math.BigDecimal;

/**
 * 资产负债健康度 VO（R004）
 * 三指标：负债率 / 流动比率 / 应急金月数
 */
public class HealthVO {

    /**
     * 负债率 = 总负债 ÷ 总资产
     */
    private Metric debtRatio;

    /**
     * 流动比率 = 现金类资产 ÷ 总负债
     */
    private Metric currentRatio;

    /**
     * 应急金月数 = 现金类资产 ÷ 近 6 个完整月月均支出
     */
    private Metric emergencyMonths;

    public Metric getDebtRatio() {
        return debtRatio;
    }

    public void setDebtRatio(Metric debtRatio) {
        this.debtRatio = debtRatio;
    }

    public Metric getCurrentRatio() {
        return currentRatio;
    }

    public void setCurrentRatio(Metric currentRatio) {
        this.currentRatio = currentRatio;
    }

    public Metric getEmergencyMonths() {
        return emergencyMonths;
    }

    public void setEmergencyMonths(Metric emergencyMonths) {
        this.emergencyMonths = emergencyMonths;
    }

    /**
     * 单指标：值 + 区间判定 + 展示 + 静态提示 + 降级原因
     */
    public static class Metric {

        /**
         * 指标值（比率 HALF_UP 2 位；月数 1 位；降级时 null）
         */
        private BigDecimal value;

        /**
         * 区间判定：excellent / good / warning（降级时 null）
         */
        private String level;

        /**
         * 展示文案："30%" / "0.30" / "6.0月"；降级时 "—"
         */
        private String display;

        /**
         * 静态提示文案（后端单一事实源）
         */
        private String hint;

        /**
         * 降级原因：no_data / no_liability / insufficient_history / no_expense；正常 null
         */
        private String degradedReason;

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getDisplay() {
            return display;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public String getHint() {
            return hint;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }

        public String getDegradedReason() {
            return degradedReason;
        }

        public void setDegradedReason(String degradedReason) {
            this.degradedReason = degradedReason;
        }
    }
}
