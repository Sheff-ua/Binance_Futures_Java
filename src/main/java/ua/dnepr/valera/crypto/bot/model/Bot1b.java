package ua.dnepr.valera.crypto.bot.model;


import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.backtest.IExchange;
import ua.dnepr.valera.crypto.bot.backtest.Statistics;
import ua.dnepr.valera.crypto.bot.backtest.StatisticsParamsDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Long Bot1b. Opens new position as soon as previous one is closed. Sets fixed percent TakeProfit and Stop Loss.
 */
public class Bot1b implements PriceListener, OrderUpdateListener {

    public static final int AMOUNT_PRECISION_BTC = 3;
    public static final BigDecimal MIN_ORDER_AMOUNT = new BigDecimal("0.001");

    private static long orderIdSequence = 1L;
    private static final Object orderIdSequenceLock = new Object();

    private final Long clientId;
    private IExchange exchange;
    private String symbol;

    private BigDecimal initialBalance;
    private int liquidatedCount = 0;
    private BigDecimal maxBalance;
    private BigDecimal maxDrawDown = BigDecimal.ZERO;
    private BigDecimal maxDrawDownAgainstInitial = BigDecimal.ZERO;

    private BigDecimal minBalance;
    private BigDecimal maxProfit = BigDecimal.ZERO;
    private BigDecimal maxProfitAgainstInitial = BigDecimal.ZERO;

    private BigDecimal balance;

    private Statistics statistics;

    private MyPosition longPosition;
    private MyPosition shortPosition;

    private BigDecimal takeProfitPercent = null;
    private BigDecimal stopLossPercent = null;

    private List<Long> shortSideOrderIds = new ArrayList<>();
    private List<Long> longSideOrderIds = new ArrayList<>();

    public Bot1b(Long clientId, String symbol, BigDecimal balance) {
        this.clientId = clientId;
        this.symbol = symbol;

        this.initialBalance = balance;
        this.minBalance = balance;
        this.maxBalance = balance;

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
        return new StatisticsParamsDTO(getStatistics(), getTakeProfitPercent(), getStopLossPercent(),
                maxBalance, maxDrawDown, maxDrawDownAgainstInitial,
                minBalance, maxProfit, maxProfitAgainstInitial,
                liquidatedCount);  // FIXME unclosed positions: close or track somehow ?!
    }

    public String getDescription() {
        return "Simple Bot, Longs open after 3% raise!, Shorts open after 3% drop!";
    }

    BigDecimal hourStartPrice = BigDecimal.ZERO;

    // TODO inject EntryPoint DecisionMaker so it could became variable for Permutation in outside cycle
    @Override
    public void onNewPrice(String symbol, BigDecimal price, long timeInMillis, Boolean isSell) {
        ZonedDateTime dateTime = Utils.parseDateTime(timeInMillis);

        if (hourStartPrice.equals(BigDecimal.ZERO) || (/*dateTime.getHour() == 1 &&*/ dateTime.getMinute() == 1)) {
            hourStartPrice = price;
        }

//        if (longPosition == null && Utils.calcXOfYInPercents(price, hourStartPrice).compareTo(new BigDecimal("96")) < 0) { // TODO some additional condition!  // long after drop (looks logical, but minus balance)
        if (longPosition == null && Utils.calcXOfYInPercents(price, hourStartPrice).compareTo(new BigDecimal("103")) < 0) { // TODO some additional condition!  // long after raise !!!
            // TODO extract variables
            BigDecimal possibleAmount = balance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);
            if (possibleAmount.compareTo(MIN_ORDER_AMOUNT) <= 0) { // No Money !!!
                System.out.println("Empty balance long! Take:" + getTakeProfitPercent() + ", Stop:" + getStopLossPercent()); // TODO
                liquidatedCount++;
                balance = initialBalance;
                return;
            }
            BigDecimal amount = balance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);
            MyOrder initialMarketOrder = new MyOrder(getNextOrderId(),
                    amount, price, false, MyOrder.Side.BUY, MyOrder.Status.NEW,
                    symbol, MyOrder.Type.MARKET, null);

            exchange.placeOrder(getClientId(), initialMarketOrder);
            longSideOrderIds.add(initialMarketOrder.getClientOrderId());
        }

//        if (shortPosition == null && Utils.calcXOfYInPercents(price, hourStartPrice).compareTo(new BigDecimal("104")) > 0) { // TODO some additional condition! // short after raise (looks logical, but minus balance)
        if (shortPosition == null && Utils.calcXOfYInPercents(price, hourStartPrice).compareTo(new BigDecimal("97")) < 0) { // TODO some additional condition! // short after drop !!!
            BigDecimal possibleAmount = balance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);
            if (possibleAmount.compareTo(MIN_ORDER_AMOUNT) <= 0) { // No Money !!!
                System.out.println("Empty balance short! Take:" + getTakeProfitPercent() + ", Stop:" + getStopLossPercent()); // TODO
                liquidatedCount++;
                balance = initialBalance;
                return;
            }

            BigDecimal amount = balance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);
            MyOrder initialMarketOrder = new MyOrder(getNextOrderId(),
                    amount, price, false, MyOrder.Side.SELL, MyOrder.Status.NEW,
                    symbol, MyOrder.Type.MARKET, null);

            exchange.placeOrder(getClientId(), initialMarketOrder);
            shortSideOrderIds.add(initialMarketOrder.getClientOrderId());
        }
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
            List<MyOrder> ordersToCancelOnExchange = new ArrayList<>();

            /*==================     LONG      ==================*/

            switch (positionChange) {
                case OPEN:
                    longPosition = new MyPosition(MyPosition.Side.LONG, symbol);

                    // should be executed only once!
                    longPosition.addOrderToHistory(updatedOrder);

                    //create, store and place additional and profit/stop orders
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

                case INCREASE:
                    break;

                case CLOSE:
                    // 1) move executed order to appropriate history
                    // 2) cancel all remaining orders on Exchange and move them to appropriate history with Status.CANCELED
                    for (MyOrder order : longPosition.getOpeningOrders()) {
                        if (order.equals(updatedOrder)) { // looks like this always be false in CLOSE case for opening orders
                            longPosition.moveOpeningOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveOpeningOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : longPosition.getTakeProfitOrders()) {
                        if (order.equals(updatedOrder)) {
                            longPosition.setCloseByProfit(true);
                            longPosition.moveTakeProfitOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveTakeProfitOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : longPosition.getStopLossOrders()) {
                        if (order.equals(updatedOrder)) {
                            longPosition.setCloseByStopLoss(true);
                            longPosition.moveStopLossOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            longPosition.moveStopLossOrderToHistory(order);
                        }
                    }
                    exchange.cancelOrdersBatchWithoutFire(getClientId(), ordersToCancelOnExchange);
                    statistics.addLongPosition(longPosition);
                    balance = balance.add(longPosition.calcRealizedPNL()).subtract(longPosition.calcCommission());
                    trackBalance();
                    longPosition = null;
                    longSideOrderIds.clear();
                    break;

                case DECREASE:
                    break;
            }
        } else if (shortSideOrderIds.contains(updatedOrder.getClientOrderId())) {
            MyPosition.PositionChange positionChange = detectPositionChange(shortPosition, updatedOrder, MyPosition.Side.SHORT);

            if (shortPosition != null) {
                shortPosition.addOrderToHistory(updatedOrder);
            }
            List<MyOrder> ordersToCancelOnExchange = new ArrayList<>();

            /*==================     SHORT      ==================*/

            switch (positionChange) {
                case OPEN:
                    shortPosition = new MyPosition(MyPosition.Side.SHORT, symbol);

                    // should be executed only once!
                    shortPosition.addOrderToHistory(updatedOrder);

                    //create, store and place additional and profit/stop orders
                    MyOrder takeProfitOrder = createTakeProfitOrder(shortPosition);
                    shortSideOrderIds.add(takeProfitOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitOrder);

                    MyOrder stopLossOrder = createStopLossOrder(shortPosition);
                    shortSideOrderIds.add(stopLossOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossOrder);

                    shortPosition.moveOpeningOrderToHistory(updatedOrder);
                    shortPosition.addTakeProfitOrder(takeProfitOrder);
                    shortPosition.addStopLossOrder(stopLossOrder);
                    break;

                case INCREASE:
                    break;

                case CLOSE:
                    // 1) move executed order to appropriate history
                    // 2) cancel all remaining orders on Exchange and move them to appropriate history with Status.CANCELED
                    for (MyOrder order : shortPosition.getOpeningOrders()) {
                        if (order.equals(updatedOrder)) { // looks like this always be false in CLOSE case for opening orders
                            shortPosition.moveOpeningOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            shortPosition.moveOpeningOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : shortPosition.getTakeProfitOrders()) {
                        if (order.equals(updatedOrder)) {
                            shortPosition.setCloseByProfit(true);
                            shortPosition.moveTakeProfitOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            shortPosition.moveTakeProfitOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : shortPosition.getStopLossOrders()) {
                        if (order.equals(updatedOrder)) {
                            shortPosition.setCloseByStopLoss(true);
                            shortPosition.moveStopLossOrderToHistory(order);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            shortPosition.moveStopLossOrderToHistory(order);
                        }
                    }

                    exchange.cancelOrdersBatchWithoutFire(getClientId(), ordersToCancelOnExchange);
                    statistics.addShortPosition(shortPosition);
                    balance = balance.add(shortPosition.calcRealizedPNL()).subtract(shortPosition.calcCommission());
                    trackBalance();
                    shortPosition = null;
                    shortSideOrderIds.clear();
                    break;

                case DECREASE:
                    break;
            }
        } else {
            //System.out.println("Unknown situation! ClientId:" + getClientId() + ", Take Profit: " + getTakeProfitPercent() + ", Stop Loss: " + getStopLossPercent() + ", Long Order Ids:"+ longSideOrderIds + ", Order" + updatedOrder);
            throw new IllegalStateException("Unknown situation!");
        }
    }


    private MyOrder createTakeProfitOrder(MyPosition position) {
        BigDecimal takeProfitPrice = MyPosition.Side.LONG.equals(position.getSide())
                ? position.getEntryPrice().add(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()))
                : position.getEntryPrice().subtract(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()));

        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(),
                takeProfitPrice, // TODO separate takes for Long \ Short
                true, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.LIMIT, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
    }

    private MyOrder createStopLossOrder(MyPosition position) {
        BigDecimal stopLossPrice = MyPosition.Side.LONG.equals(position.getSide())
                ? position.getEntryPrice().subtract(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()))  // TODO separate takes for Long \ Short
                : position.getEntryPrice().add(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()));

        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(),
                stopLossPrice,
                true, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.STOP_MARKET, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
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

    private void trackBalance() {
        if (balance.compareTo(maxBalance) > 0) { maxBalance = balance; }
        if (balance.compareTo(minBalance) < 0) { minBalance = balance; }

        BigDecimal maxDrawDownAgainstInitialCandidate = Utils.calcXOfYInPercents(initialBalance.subtract(minBalance), initialBalance);
        if (maxDrawDownAgainstInitialCandidate.compareTo(maxDrawDownAgainstInitial) > 0) {
            maxDrawDownAgainstInitial = maxDrawDownAgainstInitialCandidate;
        }

        BigDecimal maxDrawDownCandidate = Utils.calcXOfYInPercents(maxBalance.subtract(balance), maxBalance);
        if (maxDrawDownCandidate.compareTo(maxDrawDown) > 0) {
            maxDrawDown = maxDrawDownCandidate;
        }

        BigDecimal maxProfitAgainstInitialCandidate = Utils.calcXOfYInPercents(maxBalance.subtract(initialBalance), initialBalance);
        if (maxProfitAgainstInitialCandidate.compareTo(maxProfitAgainstInitial) > 0) {
            maxProfitAgainstInitial = maxProfitAgainstInitialCandidate;
        }

        BigDecimal maxProfitCandidate = Utils.calcXOfYInPercents(balance.subtract(minBalance), minBalance);
        if (maxProfitCandidate.compareTo(maxProfit) > 0) {
            maxProfit = maxProfitCandidate;
        }
    }
}
