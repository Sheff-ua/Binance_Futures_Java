package ua.dnepr.valera.crypto.bot.model;

import ua.dnepr.valera.crypto.bot.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MyPosition {

    // TODO variation margin
    private final String symbol;
    private final Side side;

//    private BigDecimal entryPrice = BigDecimal.ZERO;
//    private BigDecimal amount = BigDecimal.ZERO;
//    private Long openTime;
//    private Long lastUpdateTime;
//    private Long closeTime;
    //private BigDecimal pNL = BigDecimal.ZERO;

    private List<MyOrder> openingOrders = new ArrayList<>();
    private List<MyOrder> takeProfitOrders = new ArrayList<>();
    private List<MyOrder> stopLossOrders = new ArrayList<>();

    private List<MyOrder> openingOrdersHistory = new ArrayList<>();
    private List<MyOrder> takeProfitOrdersHistory = new ArrayList<>();
    private List<MyOrder> stopLossOrdersHistory = new ArrayList<>();

    private List<MyOrder> ordersHistory = new ArrayList<>();

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
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

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
                } else {
                    throw new IllegalStateException("Realized PnL calculation for Short position is not implemented");
                    // TODO Short
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
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    throw new IllegalStateException("Entry price calculation for Short position is not implemented");
                    // TODO Short
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
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    throw new IllegalStateException("Amount calculation for Short position is not implemented");
                    // TODO Short
                }
            }
        }
        return amountCalc;
    }

    public BigDecimal getMaxAmount() {
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        //amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    throw new IllegalStateException("Amount calculation for Short position is not implemented");
                    // TODO Short
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
        BigDecimal entryPriceCalc = BigDecimal.ZERO;
        BigDecimal amountCalc = BigDecimal.ZERO;

        for (MyOrder order : ordersHistory) {
            if (MyOrder.Status.FILLED.equals(order.getStatus())) {
                if (Side.LONG.equals(this.side)) {
                    if (order.getSide().equals(MyOrder.Side.BUY)) {
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                        }
                    } else { // SELL
                        amountCalc = amountCalc.subtract(order.getAmount());
                    }
                } else {
                    throw new IllegalStateException("Close Time calculation for Short position is not implemented");
                    // TODO Short
                }
            }
            if (amountCalc.compareTo(BigDecimal.ZERO) == 0) {
                return order.getUpdateTime();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MyPosition{" +
                "symbol='" + symbol + '\'' +
                ", side=" + side +
                ", openTime=" + Utils.formatDateTimeUTCForPrint(getOpenTime()) +
                ", closeTime=" + Utils.formatDateTimeUTCForPrint(getCloseTime()) +
                ", entryPrice=" + getEntryPrice() +
                ", maxAmount=" + getMaxAmount() +
                ", PnL=" + calcRealizedPNL() +
                ", Commission=" + calcCommission() +
                '}';
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
                        if (entryPriceCalc.compareTo(BigDecimal.ZERO) == 0) { // initial order
                            entryPriceCalc = order.getPrice();
                            amountCalc = order.getAmount();
                            result = result + "[BUY -> Entry Price: " + entryPriceCalc + ", Amount: " + amountCalc + "]";
                        } else {
                            BigDecimal resultingAmount = amountCalc.add(order.getAmount());
                            BigDecimal existingFraction = amountCalc.divide(resultingAmount, RoundingMode.DOWN);
                            BigDecimal newFraction = order.getAmount().divide(resultingAmount, RoundingMode.DOWN);

                            BigDecimal existingFractionPricePart = existingFraction.multiply(entryPriceCalc);
                            BigDecimal newFractionPricePart = newFraction.multiply(order.getPrice());

                            entryPriceCalc = existingFractionPricePart.add(newFractionPricePart);
                            amountCalc = resultingAmount;
                            result = result + "[BUY Additional -> Entry Price: " + entryPriceCalc + ", Amount: " + amountCalc + "]";
                        }
                    } else { // SELL
                        BigDecimal pnlPart = order.getPrice().subtract(entryPriceCalc).multiply(order.getAmount());
                        pNLCalc = pNLCalc.add(pnlPart);
                        amountCalc = amountCalc.subtract(order.getAmount());
                        result = result + "[SELL -> Price: " + order.getPrice() + ", Amount: " + order.getAmount() + ",  Entry Price: " + entryPriceCalc + ", Amount: " + amountCalc + ", Realized PNL: " + pNLCalc + "]";
                    }
                } else {
                    throw new IllegalStateException("Realized PnL calculation for Short position is not implemented");
                    // TODO Short
                }
            }
        }

        return result;
    }

    enum Side {
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
