package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.Utils;

import java.math.BigDecimal;

public class StatisticsParamsDTO implements Comparable<StatisticsParamsDTO> {

    private Statistics statistics;
    private BigDecimal takeProfitPercent;
    private BigDecimal stopLossPercent;

    private BigDecimal maxBalance;
    private BigDecimal maxDrawDown;
    private BigDecimal maxDrawDownAgainstInitial;

    private BigDecimal minBalance;
    private BigDecimal maxProfit;
    private BigDecimal maxProfitAgainstInitial;

    private int liquidatedCount;

    public StatisticsParamsDTO(Statistics statistics, BigDecimal takeProfitPercent, BigDecimal stopLossPercent) {
        this.statistics = statistics;
        this.takeProfitPercent = takeProfitPercent;
        this.stopLossPercent = stopLossPercent;
    }

    public StatisticsParamsDTO(Statistics statistics, BigDecimal takeProfitPercent, BigDecimal stopLossPercent,
                               BigDecimal maxBalance, BigDecimal maxDrawDown, BigDecimal maxDrawDownAgainstInitial,
                               BigDecimal minBalance, BigDecimal maxProfit, BigDecimal maxProfitAgainstInitial, int liquidatedCount) {
        this.statistics = statistics;
        this.takeProfitPercent = takeProfitPercent;
        this.stopLossPercent = stopLossPercent;
        this.maxBalance = maxBalance;
        this.maxDrawDown = maxDrawDown;
        this.maxDrawDownAgainstInitial = maxDrawDownAgainstInitial;
        this.minBalance = minBalance;
        this.maxProfit = maxProfit;
        this.maxProfitAgainstInitial = maxProfitAgainstInitial;
        this.liquidatedCount = liquidatedCount;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public BigDecimal getTakeProfitPercent() {
        return takeProfitPercent;
    }

    public BigDecimal getStopLossPercent() {
        return stopLossPercent;
    }

    @Override
    public int compareTo(StatisticsParamsDTO o) {
        return this.statistics.balanceDelta().compareTo(o.getStatistics().balanceDelta());
    }

    public String toBeautifulString() {
        return "" +
                statistics.toBeautifulString() +
                "\n      TakeProfit " + takeProfitPercent + "%" +
                "\n      StopLoss   " + stopLossPercent + "%" +
                "\n-----------------------------------------" ;
    }

    @Override
    public String toString() {
        return  statistics +

                ", maxBalance=" + Utils.formatPrice(maxBalance) +
                ", minBalance=" + Utils.formatPrice(minBalance) +
//                ", maxProfitAgainstInitial=" + Utils.formatPrice(maxProfitAgainstInitial) + "%" + // TODO looks not very useful at start, but necessarily check before Production ))
//                ", maxDrawDownAgainstInitial=" + Utils.formatPrice(maxDrawDownAgainstInitial) + "%" +
                ", maxProfit=" + Utils.formatPrice(maxProfit) + "%" +
                ", maxDrawDown=" + Utils.formatPrice(maxDrawDown) + "%" +

                ", takeProfitPercent=" + takeProfitPercent + "%" +
                ", stopLossPercent=" + stopLossPercent + "%" +
                (liquidatedCount > 0 ? "liquidatedCount=" + liquidatedCount : "")
                ;
    }
}
