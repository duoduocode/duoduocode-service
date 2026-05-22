package com.duoduocode.service.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户趋势报表VO
 */
public class AccountTrendReportVO {
    private Long accountId;
    private String accountName;
    private List<DailyBalance> dailyBalances;
    private BigDecimal maxBalance;
    private BigDecimal minBalance;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public List<DailyBalance> getDailyBalances() {
        return dailyBalances;
    }

    public void setDailyBalances(List<DailyBalance> dailyBalances) {
        this.dailyBalances = dailyBalances;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public void setMaxBalance(BigDecimal maxBalance) {
        this.maxBalance = maxBalance;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
    }
}
