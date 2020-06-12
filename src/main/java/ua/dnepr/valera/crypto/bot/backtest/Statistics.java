package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.model.MyPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Statistics {

    private List<MyPosition> longPositions = new ArrayList<>();

    public void addLongPosition(MyPosition longPosition) {
        longPositions.add(longPosition);
    }

    public BigDecimal calcLongPositionsProfit() {
        BigDecimal result = BigDecimal.ZERO;
        for (MyPosition longPosition : longPositions) {
            result = result.add(longPosition.calcRealizedPNL());
        }
        return result;
    }

    public BigDecimal calcLongPositionsCommission() {
        BigDecimal result = BigDecimal.ZERO;
        for (MyPosition longPosition : longPositions) {
            result = result.add(longPosition.calcCommission());
        }
        return result;
    }

    public long longPositionsSize() {
        return longPositions.size();
    }

    public BigDecimal balanceDelta () {
        return calcLongPositionsProfit().subtract(calcLongPositionsCommission()); // FIXME add short cases
    }

    @Override
    public String toString() {
        return "Statistics{" +
                " balanceDelta=" + balanceDelta() +
                ", positionsCount=" + longPositions.size() +
                ", longPositionsPnL=" + calcLongPositionsProfit() +
                ", longPositionsCommission=" + calcLongPositionsCommission() +
                " }";
    }
}
