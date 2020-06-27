package ua.dnepr.valera.crypto.bot.model;

import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.backtest.IExchange;
import ua.dnepr.valera.crypto.bot.backtest.Statistics;
import ua.dnepr.valera.crypto.bot.backtest.StatisticsParamsDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Long Bot1a. Opens new position as soon as previous one is closed. Sets fixed percent TakeProfit and Stop Loss.
 */
public class Bot1a implements PriceListener, OrderUpdateListener {

    public static final int AMOUNT_PRECISION_BTC = 3;
    public static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("0.001");

    private static long orderIdSequence = 1L;
    private static final Object orderIdSequenceLock = new Object();

    private final Long clientId;
    private IExchange exchange;
    private String symbol;

    private BigDecimal balance;

    private Statistics statistics;

    private MyPosition longPosition;
    private MyPosition shortPosition;

    private BigDecimal takeProfitPercent = new BigDecimal("0.5");
    private BigDecimal stopLossPercent = new BigDecimal("2");

    private List<String> shortSideOrderIds = new ArrayList<>();
    private List<Long> longSideOrderIds = new ArrayList<>();

    public Bot1a(Long clientId, String symbol, BigDecimal balance) {
        this.clientId = clientId;
        this.symbol = symbol;
        this.balance = balance;
        statistics = new Statistics();
    }

    private long getNextOrderId() {
        synchronized (orderIdSequenceLock) {
            orderIdSequence++;
        }
        return orderIdSequence;
    }

    public void setExchange(IExchange exchange) {
        this.exchange = exchange;
    }

    public void setTakeProfitPercent(BigDecimal takeProfitPercent) {
        this.takeProfitPercent = takeProfitPercent;
    }

    public BigDecimal getTakeProfitPercent() {
        return takeProfitPercent;
    }

    public BigDecimal getStopLossPercent() {
        return stopLossPercent;
    }

    public void setStopLossPercent(BigDecimal stopLossPercent) {
        this.stopLossPercent = stopLossPercent;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public StatisticsParamsDTO getStatisticsParamsDTO() {
        return new StatisticsParamsDTO(getStatistics(), getTakeProfitPercent(), getStopLossPercent());  // FIXME unclosed positions: close or track somehow ?!
    }

    public List<MyPosition> getUnclosedPositions() {
        List<MyPosition> result = new ArrayList<>();
        if (longPosition != null) {
            result.add(longPosition);
        }
        if (shortPosition != null) {
            result.add(longPosition);
        }
        return result;
    }

    // if (some condition) then open position (send market order)
    // when position opened place Take Profit order and N-additional Open orders (Step is 1% / 3 Fibonacci + Martingale levels) and Stop Loss order (1.5%). Note: stop loss volume against balance. Open orders volume against stop loss.
    // wait
    // if (Take Profit order executed) cancel all position's orders -> position closed -> add closed position to Statistics
    // if (additional Open order executed) recreate (cancel-create) Take Profit order and recreate Stop Loss order. And set new Entry Price/Update Time
    @Override
    public void onOrderUpdate(MyOrder updatedOrder) {
        // on first (market) order filled -> place take profit and stop loss
        // on additional open order recreate take profit and stop loss
        if (longSideOrderIds.contains(updatedOrder.getClientOrderId())) {
            MyPosition.PositionChange positionChange = detectPositionChange(longPosition, updatedOrder, MyPosition.Side.LONG);

            if (longPosition != null) {
                longPosition.addOrderToHistory(updatedOrder);
            }

            switch (positionChange) {
                case OPEN:
                    longPosition = new MyPosition(MyPosition.Side.LONG, symbol);

                    // should be executed only once!
                    longPosition.addOrderToHistory(updatedOrder);

                    //create, store and place additional and stop orders

                    // FIXME 1) uncomment additional orders along with INCREASE case
//                    List<MyOrder> additionalOrders = createAdditionalOpenOrders(longPosition);
//                    for (MyOrder order : additionalOrders) {
//                        longSideOrderIds.add(order.getClientOrderId());
//                    }

                    MyOrder takeProfitOrder = createTakeProfitOrder(longPosition);
                    longSideOrderIds.add(takeProfitOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitOrder);

                    MyOrder stopLossOrder = createStopLossOrder(longPosition);
                    longSideOrderIds.add(stopLossOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossOrder);

                    longPosition.moveOpeningOrderToHistory(updatedOrder);
                    longPosition.addTakeProfitOrder(takeProfitOrder);
                    longPosition.addStopLossOrder(stopLossOrder);

                    break;
                case CLOSE:
                    // 1) move executed order to appropriate history
                    // 2) cancel all remaining orders on Exchange and move them to appropriate history with Status.CANCELED
                    List<MyOrder> ordersToCancelOnExchange = new ArrayList<>();
                    for (MyOrder order : longPosition.getOpeningOrders()) {
                        if (!order.equals(updatedOrder)) { // looks like this always be true for opening orders
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveOpeningOrderToHistory(order);
                        } else {
                            longPosition.moveOpeningOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : longPosition.getTakeProfitOrders()) {
                        if (!order.equals(updatedOrder)) {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveTakeProfitOrderToHistory(order);
                        } else {
                            longPosition.moveTakeProfitOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : longPosition.getStopLossOrders()) {
                        if (!order.equals(updatedOrder)) {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveStopLossOrderToHistory(order);
                        } else {
                            longPosition.moveStopLossOrderToHistory(order);
                        }
                    }

                    exchange.cancelOrdersBatchWithoutFire(getClientId(), ordersToCancelOnExchange);
                    statistics.addLongPosition(longPosition);
                    balance = balance.add(longPosition.calcRealizedPNL()).subtract(longPosition.calcCommission());
                    longPosition = null;
                    longSideOrderIds.clear();

                    break;
                case INCREASE:
                    // calc updated Entry Price for position


                    break;
                case DECREASE:


                    break;
            }
        } else if (shortSideOrderIds.contains(updatedOrder.getClientOrderId())) {
            throw new IllegalStateException("Short is not implemented!");
        } else {
            throw new IllegalStateException("Unknown situation!");
        }
    }


    private MyOrder createTakeProfitOrder(MyPosition position) {
        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(),
                MyPosition.Side.LONG.equals(position.getSide())
                    ? position.getEntryPrice().add(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()))
                    : position.getEntryPrice().subtract(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice())), // TODO separate takes for Long \ Short
                true, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.LIMIT, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
    }

    private MyOrder createStopLossOrder(MyPosition position) {
        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(),
                MyPosition.Side.LONG.equals(position.getSide())
                ? position.getEntryPrice().subtract(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()))  // TODO separate takes for Long \ Short
                : position.getEntryPrice().add(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice())),
                true, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.STOP_MARKET, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
    }

    @Override
    public void onNewPrice(String symbol, BigDecimal price, long time, Boolean isSell) {
        if (longPosition == null) { // TODO some additional condition!

//            BigDecimal halfOfBalance = balance.divide(new BigDecimal("2"), RoundingMode.DOWN); // FIXME Full or Half
//            BigDecimal amount = halfOfBalance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN); // 50%

            BigDecimal amount = balance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);
            if (amount.compareTo(MIN_ORDER_AMOUNT) <= 0) {
                return; // No Money !!!
            }

            MyOrder initialMarketOrder = new MyOrder(getNextOrderId(),
                    amount, price, false, MyOrder.Side.BUY, MyOrder.Status.NEW,
                    symbol, MyOrder.Type.MARKET, null);

            exchange.placeOrder(getClientId(), initialMarketOrder);
            longSideOrderIds.add(initialMarketOrder.getClientOrderId());
        }


        // TODO track max drawdown / max paper profit (use for adjustment?)
    }

    public Long getClientId() {
        return clientId;
    }

    /**
     * matching of Long/Short Position with affecting Order should be done outside.
     * */
    public static MyPosition.PositionChange detectPositionChange(MyPosition position, MyOrder updatedOrder, MyPosition.Side positionSide) {
        if (MyPosition.Side.LONG.equals(positionSide)) {
            if ((position == null || !position.isOpen()) && MyOrder.Side.BUY.equals(updatedOrder.getSide())) {
                return MyPosition.PositionChange.OPEN;
            }
            if (position != null && position.isOpen() && MyOrder.Side.BUY.equals(updatedOrder.getSide())) {
                return MyPosition.PositionChange.INCREASE;
            }
            if (position != null && position.isOpen() && MyOrder.Side.SELL.equals(updatedOrder.getSide())) {
                if (position.getAmount().compareTo(updatedOrder.getAmount()) == 0) {
                    return MyPosition.PositionChange.CLOSE;
                } else {
                    return MyPosition.PositionChange.DECREASE;
                }
            }

        } else { // Short side position
            if ((position == null || !position.isOpen()) && MyOrder.Side.SELL.equals(updatedOrder.getSide())) {
                return MyPosition.PositionChange.OPEN;
            }
            if (position != null && position.isOpen() && MyOrder.Side.SELL.equals(updatedOrder.getSide())) {
                return MyPosition.PositionChange.INCREASE;
            }
            if (position != null && position.isOpen() && MyOrder.Side.BUY.equals(updatedOrder.getSide())) {
                if (position.getAmount().compareTo(updatedOrder.getAmount()) == 0) {
                    return MyPosition.PositionChange.CLOSE;
                } else {
                    return MyPosition.PositionChange.DECREASE;
                }
            }
        }

        throw new IllegalArgumentException("Unknown Position change!");
    }
}
