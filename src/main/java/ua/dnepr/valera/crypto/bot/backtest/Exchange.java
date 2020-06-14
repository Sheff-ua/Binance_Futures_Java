package ua.dnepr.valera.crypto.bot.backtest;

import com.binance.client.model.market.AggregateTradeMini;
import ua.dnepr.valera.crypto.bot.model.MyOrder;
import ua.dnepr.valera.crypto.bot.model.OrderUpdateListener;
import ua.dnepr.valera.crypto.bot.model.PriceListener;

import java.math.BigDecimal;
import java.util.*;

public class Exchange implements IExchange {

    private String symbol;

    private Map<Long, List<MyOrder>> currentOrders = new HashMap<>(); // clientId -> List MyOrder

    private List<PriceListener> priceListeners = new ArrayList<>();
    private Map<Long, OrderUpdateListener> orderUpdateListeners = new HashMap<>(); // clientId -> client
    private HistoryStorage historyStorage;
    private int historyStep = 0;
    private int historySize = 0;


    private AggregateTradeMini prevAggregateTrade;
    private AggregateTradeMini currAggregateTrade;

    public Exchange(String symbol) {
        this.symbol = symbol;
    }

    public void setHistoryStorage(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
        this.historySize = historyStorage.getHistorySize();
    }

    public BigDecimal getLastPrice () {
        return prevAggregateTrade.getPrice();
    }

    // 1) check and execute orders, and fire OrderUpdate events if executed
    // 2) fire NewPrice event
    public boolean processNext() {
        if (historyStep >= historySize) {
            return false;
        }

        currAggregateTrade = historyStorage.getStep(historyStep);
        historyStep++;

        if (prevAggregateTrade == null ) {
            prevAggregateTrade = currAggregateTrade;
            return historyStep < historySize;
        }

        // 1)
        boolean priceUp = currAggregateTrade.getPrice().compareTo(prevAggregateTrade.getPrice()) > 0;

        for (Long clientId : currentOrders.keySet()) {
            List<MyOrder> clientOrders = new ArrayList<>(currentOrders.get(clientId)); // new collection created
            for (MyOrder clientOrder : clientOrders) {
                if (clientOrder.getStatus().equals(MyOrder.Status.CANCELLED)) {
                    continue;
                }
                if (priceUp) {
                    if ((MyOrder.Type.LIMIT.equals(clientOrder.getType()) && MyOrder.Side.SELL.equals(clientOrder.getSide()))
                            || (MyOrder.Type.STOP_MARKET.equals(clientOrder.getType()) && MyOrder.Side.BUY.equals(clientOrder.getSide()))) {
                        if (prevAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) < 0 && currAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) >= 0) {
                            // order executed at clientOrder.getPrice()
                            fireOrderUpdate(clientId, clientOrder, clientOrder.getPrice(), currAggregateTrade.getTime());
                        }
                    }
                } else { // price down
                    if ((MyOrder.Type.LIMIT.equals(clientOrder.getType()) && MyOrder.Side.BUY.equals(clientOrder.getSide()))
                            || (MyOrder.Type.STOP_MARKET.equals(clientOrder.getType()) && MyOrder.Side.SELL.equals(clientOrder.getSide()))) {
                        if (prevAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) > 0 && currAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) <= 0) {
                            // order executed at clientOrder.getPrice()
                            fireOrderUpdate(clientId, clientOrder, clientOrder.getPrice(), currAggregateTrade.getTime());
                        }
                    }
                }
                if (MyOrder.Type.MARKET.equals(clientOrder.getType())) {
                    // order executed at currAggregateTrade.getPrice()
                    fireOrderUpdate(clientId, clientOrder, currAggregateTrade.getPrice(), currAggregateTrade.getTime());
                }
            }
        }

        // 2)
        fireNewPrice(currAggregateTrade);
        prevAggregateTrade = currAggregateTrade;

        return true;
    }

    // set status
    // set execution price
    // set updateTime
    // remove from current orders list
    // send to listener
    private void fireOrderUpdate(Long clientId, MyOrder clientOrder, BigDecimal executionPrice, Long executedTime) {
        MyOrder clientOrderFilled = new MyOrder(clientOrder.getClientOrderId(), clientOrder.getAmount(), executionPrice,
                clientOrder.isReduceOnly(), clientOrder.getSide(), MyOrder.Status.FILLED,
                clientOrder.getSymbol(), clientOrder.getType(), executedTime);

        List<MyOrder> clientOrders = currentOrders.get(clientId); // FIXME looks like ConcurrentModification is possible here, when additional orders will be created during update (do copy before iteration in processNext loop)

        clientOrders.remove(clientOrder);
        OrderUpdateListener client = orderUpdateListeners.get(clientId);
        client.onOrderUpdate(clientOrderFilled);
    }

    private void fireNewPrice(AggregateTradeMini aggregateTrade) {
        for (PriceListener priceListener : priceListeners) {
            priceListener.onNewPrice(this.symbol, aggregateTrade.getPrice(), aggregateTrade.getTime(), null);
        }
    }

    public void cancelOrdersBatchWithoutFire(Long clientId, List<MyOrder> ordersToCancel) {
        List<MyOrder> clientOrders = currentOrders.get(clientId);
        Iterator<MyOrder> clientOrdersIt = clientOrders.iterator();

        while(clientOrdersIt.hasNext()) { // TODO replace with #replaceIf
            MyOrder clientOrder = clientOrdersIt.next();
            if (ordersToCancel.contains(clientOrder)) {
                clientOrder.setStatus(MyOrder.Status.CANCELLED);
                clientOrdersIt.remove();
            }
        }
    }

    public void placeOrder(Long clientId, MyOrder myOrder) {
        if (MyOrder.Type.LIMIT.equals(myOrder.getType())) {
            if (MyOrder.Side.BUY.equals(myOrder.getSide()) && currAggregateTrade.getPrice().compareTo(myOrder.getPrice()) < 0) {
                throw new IllegalArgumentException("Tried to place Limit Buy with price higher then Market price");
            }
            if (MyOrder.Side.SELL.equals(myOrder.getSide()) && currAggregateTrade.getPrice().compareTo(myOrder.getPrice()) > 0) {
                throw new IllegalArgumentException("Tried to place Limit Sell with price lower then Market price");
            }
        }

        List<MyOrder> clientOrders = currentOrders.get(clientId);
        if (clientOrders == null) {
            clientOrders = new ArrayList<>();
            currentOrders.put(clientId, clientOrders);
        }
        if (!clientOrders.contains(myOrder)) {
            clientOrders.add(myOrder);
        } else {
            System.out.println("Unknown situation! WTF!!!");
            //throw new IllegalStateException("Unknown situation! Duplicate order!");
        }

    }

    public void addPriceListener(PriceListener priceListener) {
        priceListeners.add(priceListener);
    }

    public void addOrderUpdateListener(Long clientId, OrderUpdateListener orderUpdateListener) {
        orderUpdateListeners.put(clientId, orderUpdateListener);
    }

    public List<StatisticsParamsDTO> getResultingStatisticsList() {
        List<StatisticsParamsDTO> result = new ArrayList<>();
        for (OrderUpdateListener bot : orderUpdateListeners.values()) {
            result.add(bot.getStatisticsParamsDTO());
        }
        return result;
    }

    public int calcPercent() {
        return historyStorage.calcPercent(historyStep);
    }


//    public void reset() {
//        orderUpdateListeners.clear();
//        priceListeners.clear();
//        currentOrders.clear();
//        iterationStep = 0;
//        prevAggregateTrade = null;
//
//        historyStorage.rewind();
//    }

}
