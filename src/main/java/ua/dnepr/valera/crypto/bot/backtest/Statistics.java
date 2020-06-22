package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.model.MyPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Statistics {

    private List<MyPosition> longPositions = new ArrayList<>();
    private List<MyPosition> shortPositions = new ArrayList<>();

    public void addLongPosition(MyPosition longPosition) {
        longPositions.add(longPosition);
    }

    public void addShortPosition(MyPosition shortPosition) {
        shortPositions.add(shortPosition);
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

    public BigDecimal calcShortPositionsProfit() {
        BigDecimal result = BigDecimal.ZERO;
        for (MyPosition shortPosition : shortPositions) {
            result = result.add(shortPosition.calcRealizedPNL());
        }
        return result;
    }

    public BigDecimal calcShortPositionsCommission() {
        BigDecimal result = BigDecimal.ZERO;
        for (MyPosition shortPosition : shortPositions) {
            result = result.add(shortPosition.calcCommission());
        }
        return result;
    }

    public int calcPositiveLongPositions() {
        int result = 0;
        for (MyPosition longPosition : longPositions) {
            if (longPosition.calcRealizedPNL().compareTo(BigDecimal.ZERO) > 0) {
                result++;
            }
        }
        return result;
    }

    public int calcNegativeLongPositions() {
        int result = 0;
        for (MyPosition longPosition : longPositions) {
            if (longPosition.calcRealizedPNL().compareTo(BigDecimal.ZERO) <= 0) {
                result++;
            }
        }
        return result;
    }

    public int calcPositiveShortPositions() {
        int result = 0;
        for (MyPosition shortPosition : shortPositions) {
            if (shortPosition.calcRealizedPNL().compareTo(BigDecimal.ZERO) > 0) {
                result++;
            }
        }
        return result;
    }

    public int calcNegativeShortPositions() {
        int result = 0;
        for (MyPosition shortPosition : shortPositions) {
            if (shortPosition.calcRealizedPNL().compareTo(BigDecimal.ZERO) <= 0) {
                result++;
            }
        }
        return result;
    }

    public int calcLongClosedByProfit() {
        int result = 0;
        for (MyPosition longPosition : longPositions) {
            if (longPosition.isCloseByProfit()) {
                result++;
            }
        }
        return result;
    }

    public int calcLongClosedByStopLoss() {
        int result = 0;
        for (MyPosition longPosition : longPositions) {
            if (longPosition.isCloseByStopLoss()) {
                result++;
            }
        }
        return result;
    }

    public int calcShortClosedByProfit() {
        int result = 0;
        for (MyPosition shortPosition : shortPositions) {
            if (shortPosition.isCloseByProfit()) {
                result++;
            }
        }
        return result;
    }

    public int calcShortClosedByStopLoss() {
        int result = 0;
        for (MyPosition shortPosition : shortPositions) {
            if (shortPosition.isCloseByStopLoss()) {
                result++;
            }
        }
        return result;
    }

    public int calcLongWithAllOpenOrdersExecuted() {
        int result = 0;
        for (MyPosition position : longPositions) {
            if (position.isAllOpeningOrdersExecuted()) {
                result++;
            }
        }
        return result;
    }

    public int calcShortWithAllOpenOrdersExecuted() {
        int result = 0;
        for (MyPosition position : shortPositions) {
            if (position.isAllOpeningOrdersExecuted()) {
                result++;
            }
        }
        return result;
    }

    public BigDecimal balanceDelta () {
        return calcLongPositionsProfit().subtract(calcLongPositionsCommission()).add(calcShortPositionsProfit()).subtract(calcShortPositionsCommission());
    }

    public String toBeautifulString() {
        return "Statistics:" +
                "\n              Balance Delta    " + Utils.formatPrice(balanceDelta()) +
                "\n              -----            " +
                "\n              Long (+/-)       " + longPositions.size() + " ( " + calcPositiveLongPositions() + " / " + calcNegativeLongPositions() + " ) " +
                "\n              Long PnL         " + Utils.formatPrice(calcLongPositionsProfit()) +
                "\n              Long Commission  " + Utils.formatPrice(calcLongPositionsCommission()) +
                "\n              -----            " +
                "\n              Short (+/-)      " + shortPositions.size() + " ( " + calcPositiveShortPositions() + " / " + calcNegativeShortPositions() + " ) " +
                "\n              Short PnL        " + Utils.formatPrice(calcShortPositionsProfit()) +
                "\n              Short Commission " + Utils.formatPrice(calcShortPositionsCommission()) +
                "\n              -----            " +
                "";
    }

    @Override
    public String toString() {
        return "Statistics{" +
                " balanceDelta=" + Utils.formatPrice(balanceDelta()) +
                ", positions=" + (longPositions.size() + shortPositions.size()) +
                ", longs=" + longPositions.size() +
                ", LongsInProfit=" + calcLongClosedByProfit() +
                ", LongsInLoss=" + calcLongClosedByStopLoss() +
                ", shorts=" + shortPositions.size() +
                ", ShortsInProfit=" + calcShortClosedByProfit() +
                ", ShortsInLoss=" + calcShortClosedByStopLoss() +
                ", longPnL=" + Utils.formatPrice(calcLongPositionsProfit()) +
                ", longCommission=" + Utils.formatPrice(calcLongPositionsCommission()) +
                ", shortPnL=" + Utils.formatPrice(calcShortPositionsProfit()) +
                ", shortCommission=" + Utils.formatPrice(calcShortPositionsCommission()) +



//                ", LongWithAllOpenOrdersExecuted=" + calcLongWithAllOpenOrdersExecuted() + // TODO makes sense only for take=1%, stop=2% and fixed money take/stop
//                ", ShortWithAllOpenOrdersExecuted=" + calcShortWithAllOpenOrdersExecuted() +
                " }";
    }
}
