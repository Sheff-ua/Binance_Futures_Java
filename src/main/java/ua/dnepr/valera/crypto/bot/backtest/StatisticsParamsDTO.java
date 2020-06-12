package ua.dnepr.valera.crypto.bot.backtest;

import java.math.BigDecimal;

public class StatisticsParamsDTO implements Comparable {

    private Statistics statistics;
    private BigDecimal takeProfitPercent;
    private BigDecimal stopLossPercent;


    public StatisticsParamsDTO(Statistics statistics, BigDecimal takeProfitPercent, BigDecimal stopLossPercent) {
        this.statistics = statistics;
        this.takeProfitPercent = takeProfitPercent;
        this.stopLossPercent = stopLossPercent;
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
    public int compareTo(Object o) {
        return this.statistics.balanceDelta().compareTo(((StatisticsParamsDTO)o).getStatistics().balanceDelta());
    }

    @Override
    public String toString() {
        return "StatisticsParamsDTO{" +
                "statistics=" + statistics +
                ", takeProfitPercent=" + takeProfitPercent +
                ", stopLossPercent=" + stopLossPercent +
                '}';
    }
}
