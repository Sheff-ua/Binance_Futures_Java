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
 * Long Bot3.
 *
 */
public class Bot3 implements PriceListener, OrderUpdateListener {

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

    public Bot3(Long clientId, String symbol, BigDecimal balance) {
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

    BigDecimal dayStartPrice = BigDecimal.ZERO;

    // TODO inject EntryPoint DecisionMaker so it could became variable for Permutation in outside cycle
    @Override
    public void onNewPrice(String symbol, BigDecimal price, long timeInMillis, Boolean isSell) {
        ZonedDateTime dateTime = Utils.parseDateTime(timeInMillis);

        if (dayStartPrice.equals(BigDecimal.ZERO) || (dateTime.getHour() == 1 && dateTime.getMinute() == 1)) {
            dayStartPrice = price;
        }
//        if (dateTime.getMinute() == 59 && dayStartPrice.compareTo(BigDecimal.ZERO) != 0) {
//            dayStartPrice = BigDecimal.ZERO;
//        }



        // TODO different balance for Long/Short positions or different Long/Short Bots
        if (longPosition == null && Utils.calcXOfYInPercents(price, dayStartPrice).compareTo(new BigDecimal("99")) < 0) { // TODO some additional condition!
//        if (longPosition == null && Utils.calcXOfYInPercents(price, dayStartPrice).compareTo(new BigDecimal("101")) > 0) { // TODO some additional condition!
            BigDecimal tenPercentsOfBalance = Utils.calcXPercentsFromY(new BigDecimal(10), balance);
            BigDecimal amount = tenPercentsOfBalance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN); // 10%

            if (amount.compareTo(MIN_ORDER_AMOUNT) <= 0) { // No Money !!!
                System.out.println("Empty balance!"); // TODO
                liquidatedCount++;
                balance = initialBalance;
            }

            MyOrder initialMarketOrder = new MyOrder(getNextOrderId(),
                    amount, price, false, MyOrder.Side.BUY, MyOrder.Status.NEW,
                    symbol, MyOrder.Type.MARKET, null);
            exchange.placeOrder(getClientId(), initialMarketOrder);
            longSideOrderIds.add(initialMarketOrder.getClientOrderId());
        }

        if (shortPosition == null && Utils.calcXOfYInPercents(price, dayStartPrice).compareTo(new BigDecimal("101")) > 0) { // TODO some additional condition!
//        if (shortPosition == null && Utils.calcXOfYInPercents(price, dayStartPrice).compareTo(new BigDecimal("99")) < 0) { // TODO some additional condition!
            BigDecimal tenPercentsOfBalance = Utils.calcXPercentsFromY(new BigDecimal(10), balance);
            BigDecimal amount = tenPercentsOfBalance.divide(price, AMOUNT_PRECISION_BTC, RoundingMode.DOWN);

            if (amount.compareTo(MIN_ORDER_AMOUNT) <= 0) {  // No Money !!!
                System.out.println("Empty balance!"); // TODO
                liquidatedCount++;
                balance = initialBalance;
            }

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

                    List<MyOrder> additionalOrders = createAdditionalOpenOrders(longPosition);
                    for (MyOrder additionalOrder : additionalOrders) {
                        longSideOrderIds.add(additionalOrder.getClientOrderId());
                        exchange.placeOrder(getClientId(), additionalOrder);
                    }

                    MyOrder takeProfitOrder = createTakeProfitOrder(longPosition, null);
                    longPosition.setExpectedProfit(calcExpectedProfit(longPosition, takeProfitOrder));
                    longSideOrderIds.add(takeProfitOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitOrder);


                    MyOrder stopLossOrder = createStopLossOrder(longPosition, null);
                    longPosition.setExpectedLoss(calcExpectedLoss(longPosition, stopLossOrder));
                    longSideOrderIds.add(stopLossOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossOrder);

                    longPosition.moveOpeningOrderToHistory(updatedOrder);
                    for (MyOrder additionalOrder : additionalOrders) {
                        longPosition.addOpeningOrder(additionalOrder);
                    }
                    longPosition.addTakeProfitOrder(takeProfitOrder);
                    longPosition.addStopLossOrder(stopLossOrder);

                    longPosition.setAllOpeningOrdersExecuted(longPosition.getOpeningOrders().size() == 0);
                    break;

                case INCREASE:
                    // calc updated Entry Price for position
                    for (MyOrder order : longPosition.getOpeningOrders()) {
                        if (order.equals(updatedOrder)) { // looks like this always be true for opening orders
                            longPosition.moveOpeningOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : longPosition.getTakeProfitOrders()) {
                        ordersToCancelOnExchange.add(order);
                        order.setStatus(MyOrder.Status.CANCELLED);
                        order.setUpdateTime(updatedOrder.getUpdateTime());
                        longPosition.moveTakeProfitOrderToHistory(order);
                    }
                    for (MyOrder order : longPosition.getStopLossOrders()) {
                        ordersToCancelOnExchange.add(order);
                        order.setStatus(MyOrder.Status.CANCELLED);
                        order.setUpdateTime(updatedOrder.getUpdateTime());
                        longPosition.moveStopLossOrderToHistory(order);
                    }

                    exchange.cancelOrdersBatchWithoutFire(getClientId(), ordersToCancelOnExchange);

                    MyOrder takeProfitIncreasedOrder = createTakeProfitOrder(longPosition, longPosition.getExpectedProfit());
                    longSideOrderIds.add(takeProfitIncreasedOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitIncreasedOrder);

                    MyOrder stopLossIncreasedOrder = createStopLossOrder(longPosition, longPosition.getExpectedLoss());
                    longSideOrderIds.add(stopLossIncreasedOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossIncreasedOrder);

                    longPosition.addTakeProfitOrder(takeProfitIncreasedOrder);
                    longPosition.addStopLossOrder(stopLossIncreasedOrder);

                    longPosition.setAllOpeningOrdersExecuted(longPosition.getOpeningOrders().size() == 0); // if all opening orders already moved to history
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
                    List<MyOrder> additionalOrders = createAdditionalOpenOrders(shortPosition);
                    for (MyOrder additionalOrder : additionalOrders) {
                        shortSideOrderIds.add(additionalOrder.getClientOrderId());
                        exchange.placeOrder(getClientId(), additionalOrder);
                    }

                    MyOrder takeProfitOrder = createTakeProfitOrder(shortPosition, null);
                    shortPosition.setExpectedProfit(calcExpectedProfit(shortPosition, takeProfitOrder));
                    shortSideOrderIds.add(takeProfitOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitOrder);

                    MyOrder stopLossOrder = createStopLossOrder(shortPosition, null);
                    shortPosition.setExpectedLoss(calcExpectedLoss(shortPosition, stopLossOrder));
                    shortSideOrderIds.add(stopLossOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossOrder);

                    shortPosition.moveOpeningOrderToHistory(updatedOrder);
                    for (MyOrder additionalOrder : additionalOrders) {
                        shortPosition.addOpeningOrder(additionalOrder);
                    }
                    shortPosition.addTakeProfitOrder(takeProfitOrder);
                    shortPosition.addStopLossOrder(stopLossOrder);

                    shortPosition.setAllOpeningOrdersExecuted(shortPosition.getOpeningOrders().size() == 0);
                    break;

                case INCREASE:
                    // calc updated Entry Price for position
                    for (MyOrder order : shortPosition.getOpeningOrders()) {
                        if (order.equals(updatedOrder)) { // looks like this always be true for opening orders
                            shortPosition.moveOpeningOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : shortPosition.getTakeProfitOrders()) {
                        ordersToCancelOnExchange.add(order);
                        order.setStatus(MyOrder.Status.CANCELLED);
                        order.setUpdateTime(updatedOrder.getUpdateTime());
                        shortPosition.moveTakeProfitOrderToHistory(order);
                    }
                    for (MyOrder order : shortPosition.getStopLossOrders()) {
                        ordersToCancelOnExchange.add(order);
                        order.setStatus(MyOrder.Status.CANCELLED);
                        order.setUpdateTime(updatedOrder.getUpdateTime());
                        shortPosition.moveStopLossOrderToHistory(order);
                    }

                    exchange.cancelOrdersBatchWithoutFire(getClientId(), ordersToCancelOnExchange);

                    MyOrder takeProfitIncreasedOrder = createTakeProfitOrder(shortPosition, shortPosition.getExpectedProfit());
                    shortSideOrderIds.add(takeProfitIncreasedOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), takeProfitIncreasedOrder);

                    MyOrder stopLossIncreasedOrder = createStopLossOrder(shortPosition, shortPosition.getExpectedLoss());
                    shortSideOrderIds.add(stopLossIncreasedOrder.getClientOrderId());
                    exchange.placeOrder(getClientId(), stopLossIncreasedOrder);

                    shortPosition.addTakeProfitOrder(takeProfitIncreasedOrder);
                    shortPosition.addStopLossOrder(stopLossIncreasedOrder);

                    shortPosition.setAllOpeningOrdersExecuted(shortPosition.getOpeningOrders().size() == 0);
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
                            shortPosition.moveTakeProfitOrderToHistory(order);
                            shortPosition.setCloseByProfit(true);
                        } else {
                            ordersToCancelOnExchange.add(order);
                            order.setStatus(MyOrder.Status.CANCELLED);
                            order.setUpdateTime(updatedOrder.getUpdateTime());
                            shortPosition.moveTakeProfitOrderToHistory(order);
                        }
                    }
                    for (MyOrder order : shortPosition.getStopLossOrders()) {
                        if (order.equals(updatedOrder)) {
                            shortPosition.moveStopLossOrderToHistory(order);
                            shortPosition.setCloseByStopLoss(true);
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


    // FIXME looks like we need to take Commission into account during additional (and maybe even during initial) orders creation
    private List<MyOrder> createAdditionalOpenOrders(MyPosition position) {
        List<MyOrder> orders = new ArrayList<>();

        // 1
        BigDecimal twentyPercentsOfBalance = Utils.calcXPercentsFromY(new BigDecimal(20), balance);
        BigDecimal entryPriceMinusOnePercent = position.getSide().equals(MyPosition.Side.LONG)
                ? position.getEntryPrice().subtract(Utils.calcXPercentsFromY(new BigDecimal("1"), position.getEntryPrice()))
                : position.getEntryPrice().add(Utils.calcXPercentsFromY(new BigDecimal("1"), position.getEntryPrice()));
        BigDecimal amount = twentyPercentsOfBalance.divide(entryPriceMinusOnePercent, AMOUNT_PRECISION_BTC, RoundingMode.DOWN); // 20%

        MyOrder order = new MyOrder(getNextOrderId(), amount, entryPriceMinusOnePercent,
                false, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.BUY : MyOrder.Side.SELL,
                MyOrder.Status.NEW, symbol, MyOrder.Type.LIMIT, null); // TODO get time from last AggTrade via #onNewPrice()
        orders.add(order);

        // 2
        BigDecimal thirtyPercentsOfBalance = Utils.calcXPercentsFromY(new BigDecimal(30), balance);
        BigDecimal entryPriceMinusTwoPercents = position.getSide().equals(MyPosition.Side.LONG)
                ? position.getEntryPrice().subtract(Utils.calcXPercentsFromY(new BigDecimal("2"), position.getEntryPrice()))
                : position.getEntryPrice().add(Utils.calcXPercentsFromY(new BigDecimal("2"), position.getEntryPrice()));
        BigDecimal amount2 = thirtyPercentsOfBalance.divide(entryPriceMinusTwoPercents, AMOUNT_PRECISION_BTC, RoundingMode.DOWN); // 20%

        MyOrder order2 = new MyOrder(getNextOrderId(), amount2, entryPriceMinusTwoPercents,
                false, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.BUY : MyOrder.Side.SELL,
                MyOrder.Status.NEW, symbol, MyOrder.Type.LIMIT, null); // TODO get time from last AggTrade via #onNewPrice()
        orders.add(order2);
        return orders;
    }

    private MyOrder createTakeProfitOrder(MyPosition position, BigDecimal expectedProfit) {
//        // FIXME variant with fixed percent Take Profit against New Entry Price
//        BigDecimal takeProfitPrice = MyPosition.Side.LONG.equals(position.getSide())
//                ? position.getEntryPrice().add(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()))
//                : position.getEntryPrice().subtract(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()));

        // FIXME variant with fixed money Take Profit against initial Expected Profit
        BigDecimal takeProfitPrice;
        if (expectedProfit == null) {
            if (MyPosition.Side.LONG.equals(position.getSide())) {
                takeProfitPrice = position.getEntryPrice().add(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()));
            } else {
                takeProfitPrice = position.getEntryPrice().subtract(Utils.calcXPercentsFromY(takeProfitPercent, position.getEntryPrice()));
            }
        } else { // Average
            takeProfitPrice = Utils.calcNewTakeForNewEntryAndAmount(position.getSide(), expectedProfit, position.getEntryPrice(), position.getAmount());
        }

        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(),
                takeProfitPrice, // TODO separate takes for Long \ Short
                true, MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.LIMIT, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
    }

    private MyOrder createStopLossOrder(MyPosition position, BigDecimal expectedLoss) {
        // FIXME variant with fixed percent Stop Loss against New Entry Price
        BigDecimal stopLossPrice = MyPosition.Side.LONG.equals(position.getSide())
                ? position.getEntryPrice().subtract(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()))
                : position.getEntryPrice().add(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()));

//        // FIXME variant with fixed money Stop Loss against initial Expected Profit
//        BigDecimal stopLossPrice;
//        if (expectedLoss == null) {
//            if (MyPosition.Side.LONG.equals(position.getSide())) {
//                stopLossPrice = position.getEntryPrice().subtract(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()));
//            } else {
//                stopLossPrice = position.getEntryPrice().add(Utils.calcXPercentsFromY(stopLossPercent, position.getEntryPrice()));
//            }
//        } else { // Average
//            stopLossPrice = Utils.calcNewStopForNewEntryAndAmount(position.getSide(), expectedLoss, position.getEntryPrice(), position.getAmount());
//        }

        MyOrder order = new MyOrder(getNextOrderId(), position.getAmount(), stopLossPrice,true,
                MyPosition.Side.LONG.equals(position.getSide()) ? MyOrder.Side.SELL: MyOrder.Side.BUY,
                MyOrder.Status.NEW, symbol, MyOrder.Type.STOP_MARKET, null); // TODO get time from last AggTrade via #onNewPrice()
        return order;
    }

    public Long getClientId() {
        return clientId;
    }

    /**
     * It is expected that position.amount equals firstTakeProfitOrder.amount
     */
    private BigDecimal calcExpectedProfit(MyPosition position, MyOrder firstTakeProfitOrder) {
        BigDecimal result;
        if (position.getSide().equals(MyPosition.Side.LONG)) {
            result = firstTakeProfitOrder.getPrice().subtract(position.getEntryPrice()).multiply(firstTakeProfitOrder.getAmount());
        } else {
            result = position.getEntryPrice().subtract(firstTakeProfitOrder.getPrice()).multiply(firstTakeProfitOrder.getAmount());
        }
        return result;
    }

    /**
     * It is expected that position.amount equals firstStopLossOrder.amount
     */
    private BigDecimal calcExpectedLoss(MyPosition position, MyOrder firstStopLossOrder) {
        BigDecimal result;
        if (position.getSide().equals(MyPosition.Side.LONG)) {
            result = firstStopLossOrder.getPrice().subtract(position.getEntryPrice()).multiply(firstStopLossOrder.getAmount()).abs();
        } else {
            result = position.getEntryPrice().subtract(firstStopLossOrder.getPrice()).multiply(firstStopLossOrder.getAmount()).abs();
        }
        return result;
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
