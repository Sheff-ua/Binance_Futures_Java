package ua.dnepr.valera.crypto.bot.model;

import ua.dnepr.valera.crypto.bot.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MyPosition {

    public static int DIVIDE_SCALE = 6;

    // TODO variation margin
    private final String symbol;
    private final Side side;

//    private BigDecimal entryPrice = BigDecimal.ZERO;
//    private BigDecimal amount = BigDecimal.ZERO;
//    private Long openTime;
//    private Long lastUpdateTime;
//    private Long closeTime;
    //private BigDecimal pNL = BigDecimal.ZERO;

    private BigDecimal expectedProfit = null;
    private BigDecimal expectedLoss = null;

    private List<MyOrder> openingOrders = new ArrayList<>();
    private List<MyOrder> takeProfitOrders = new ArrayList<>();
    private List<MyOrder> stopLossOrders = new ArrayList<>();

    private List<MyOrder> openingOrdersHistory = new ArrayList<>();
    private List<MyOrder> takeProfitOrdersHistory = new ArrayList<>();
    private List<MyOrder> stopLossOrdersHistory = new ArrayList<>();

    private List<MyOrder> ordersHistory = new ArrayList<>();

    private boolean allOpeningOrdersExecuted = false;
    private boolean closeByStopLoss = false;
    private boolean closeByProfit = false;

    //private List<MyPosition> subPositions = new ArrayList<>();

    public MyPosition(Side side, String symbol) {
        this.side = side;
        this.symbol = symbol;
    }

    public boolean isOpen() {
        return getAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isOpening() {
        return openingOrders.size() > 0;
    }

    public boolean isAllOpeningOrdersExecuted() {
        return allOpeningOrdersExecuted;
    }

    public void setAllOpeningOrdersExecuted(boolean allOpeningOrdersExecuted) {
        this.allOpeningOrdersExecuted = allOpeningOrdersExecuted;
    }

    public boolean isCloseByStopLoss() {
        return closeByStopLoss;
    }

    public void setCloseByStopLoss(boolean closeByStopLoss) {
        this.closeByStopLoss = closeByStopLoss;
    }

    public boolean isCloseByProfit() {
        return closeByProfit;
    }

    public void setCloseByProfit(boolean closeByProfit) {
        this.closeByProfit = closeByProfit;
    }

    /* realized commission */
    public BigDecimal calcCommission() {
        BigDecimal commissionCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (order.getType().equals(MyOrder.Type.MARKET) || order.getType().equals(MyOrder.Type.STOP_MARKET)) {
                    commissionCalc = commissionCalc.add(order.getPrice().multiply(order.getAmount().multiply(new BigDecimal("0.0004"))));
                } else if (order.getType().equals(MyOrder.Type.LIMIT)) {
                    commissionCalc = commissionCalc.add(order.getPrice().multiply(order.getAmount().multiply(new BigDecimal("0.0002"))));
                }
            }
        }

        return commissionCalc;
    }

    public BigDecimal calcRealizedPNL() {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;
        BigDecimal pNLCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        BigDecimal pnlPart = order.getPrice().subtract(entryPriceCalc).multiply(order.getAmount());
                        pNLCalc = pNLCalc.add(pnlPart);
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else { // SHORT
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // BUY
                        BigDecimal pnlPart = entryPriceCalc.subtract(order.getPrice()).multiply(order.getAmount()); // FIXME fixed as reverse of price to subtract from
                        pNLCalc = pNLCalc.add(pnlPart);
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                }
            }
        }

        return pNLCalc;
    }

    public BigDecimal calcUnRealizedPNL(BigDecimal price) {
        BigDecimal amount = getAmount();
        BigDecimal entryPrice = getEntryPrice();
        if (Side.LONG.equals(this.side)) {
            return price.subtract(entryPrice).multiply(amount);
        } else {
            //return price.subtract(entryPrice).multiply(amount);
            throw new IllegalStateException("Unrealized PnL calculation for Short position is not implemented");
        }
    }

    public Side getSide() {
        return side;
    }

    public String getSymbol() {
        return symbol;
    }

    public void addOpeningOrder(MyOrder order) {
        openingOrders.add(order);
    }

    public void addTakeProfitOrder(MyOrder order) {
        takeProfitOrders.add(order);
    }

    public void addStopLossOrder(MyOrder order) {
        stopLossOrders.add(order);
    }

    public List<MyOrder> getOpeningOrders() {
        return new ArrayList<>(openingOrders);
    }

    public List<MyOrder> getTakeProfitOrders() {
        return new ArrayList<>(takeProfitOrders);
    }

    public List<MyOrder> getStopLossOrders() {
        return new ArrayList<>(stopLossOrders);
    }

    public void addOrderToHistory(MyOrder order) {
        ordersHistory.add(order);
    }

    public void moveOpeningOrderToHistory(MyOrder order) {
        openingOrders.remove(order);
        openingOrdersHistory.add(order);
    }

    public void moveTakeProfitOrderToHistory(MyOrder order) {
        takeProfitOrders.remove(order);
        takeProfitOrdersHistory.add(order);
    }

    public void moveStopLossOrderToHistory(MyOrder order) {
        stopLossOrders.remove(order);
        stopLossOrdersHistory.add(order);
    }

    public BigDecimal getEntryPrice() {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) { // LONG
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else { // SHORT
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // BUY
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                }
            }
        }

        return entryPriceCalc;
    }

    public BigDecimal getAmount() {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                }
            }
        }
        return amountCalc;
    }

    public BigDecimal getMaxAmount() {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;
        BigDecimal maxAmountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                            if (amountCalc.compareTo(maxAmountCalc) > 0) {
                                maxAmountCalc = amountCalc;
                            }
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                            if (amountCalc.compareTo(maxAmountCalc) > 0) {
                                maxAmountCalc = amountCalc;
                            }
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                }
            }
        }
        return amountCalc;
    }

    public Long getOpenTime() {
        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        return order.getUpdateTime();
                    }
                } else {
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        return order.getUpdateTime();
                    }
                }
            }
        }
        return null;
    }

    public Long getLastUpdateTime() {
        Long lastUpdatedTime = null;
        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                lastUpdatedTime = order.getUpdateTime();
            }
        }
        return lastUpdatedTime;
    }

    public Long getCloseTime() {
        BigDecimal amountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            amountCalc = order.getAmount();
                        } else {
                            amountCalc = amountCalc.add(order.getAmount());
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    if (order.getSide().equals(MyOrder.Side.SELL)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            amountCalc = order.getAmount();
                        } else {
                            amountCalc = amountCalc.add(order.getAmount());
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                }
            }
            if (amountCalc.compareTo(BigDecimal.ZERO) == 0) {
                return order.getUpdateTime();
            }
        }
        return null;
    }

    public String printEntryPriceAmountHistory () {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;
        BigDecimal pNLCalc = BigDecimal.ZERO;
        String result = "";

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (amountCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                            result = result + "[BUY            -> Entry Price: " + Utils.formatPrice(entryPriceCalc) + ", Amount: " + amountCalc + "]\n";
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, DIVIDE_SCALE, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                            result = result + "[BUY Additional -> Entry Price: " + Utils.formatPrice(entryPriceCalc) + ", Amount: " + amountCalc + "]\n";
                        }
                    } else { // SELL
                        BigDecimal pnlPart = order.getPrice().subtract(entryPriceCalc).multiply(order.getAmount());
                        pNLCalc = pNLCalc.add(pnlPart);
                        amountCalc = amountCalc.subtract(order.getAmount());
                        result = result + "[SELL           -> Entry Price: " + Utils.formatPrice(entryPriceCalc) + ", Amount: " + amountCalc + ",  Price: " + Utils.formatPrice(order.getPrice()) + ", Amount: " + order.getAmount() + ", Realized PNL: " + Utils.formatPrice(pNLCalc) + "]\n";
                    }
                } else {
                    throw new IllegalStateException("Realized PnL calculation for Short position is not implemented");
                    // TODO Short
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "MyPosition{" +
                "symbol='" + symbol + '\'' +
                ", side=" + side +
                ", openTime=" + Utils.formatDateTimeUTCForPrint(getOpenTime()) +
                ", closeTime=" + Utils.formatDateTimeUTCForPrint(getCloseTime()) +
                ", entryPrice=" + Utils.formatPrice(getEntryPrice()) +
                ", amount=" + getAmount() +
                ", maxAmount=" + getMaxAmount() +
                ", PnL=" + Utils.formatPrice(calcRealizedPNL()) +
                ", Commission=" + Utils.formatPrice(calcCommission()) +
                '}';
    }

    public BigDecimal getExpectedProfit() {
        return expectedProfit;
    }

    public void setExpectedProfit(BigDecimal expectedProfit) {
        this.expectedProfit = expectedProfit;
    }

    public BigDecimal getExpectedLoss() {
        return expectedLoss;
    }

    public void setExpectedLoss(BigDecimal expectedLoss) {
        this.expectedLoss = expectedLoss;
    }

    public enum Side {
        LONG,
        SHORT
    }

    public enum PositionChange {
        OPEN,
        CLOSE,
        INCREASE,
        DECREASE,

//        NOTHING, // maybe just new limit order is placed
//        INVERSION_TO_BUY,
//        INVERSION_TO_SELL
    }
}
