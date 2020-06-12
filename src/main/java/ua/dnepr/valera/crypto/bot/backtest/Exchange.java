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
    private HistoryPlayer historyPlayer;
    private long iterationStep = 0;


    private AggregateTradeMini prevAggregateTrade;
    private AggregateTradeMini currAggregateTrade;

    public Exchange(String from, String to, String symbol) {
        this.symbol = symbol;
        historyPlayer = new HistoryPlayer(symbol, true); // FIXME reduced history is used
        historyPlayer.init(from, to);
    }

    public BigDecimal getlastPrice () {
        return prevAggregateTrade.getPrice();
    }

    // 1) check and execute orders, and fire OrderUpdate events if executed
    // 2) fire NewPrice event
    public boolean processNext() {
        if (!historyPlayer.hasNext()) {
            return false;
        }
        currAggregateTrade = historyPlayer.getNext();
        iterationStep++;

        if (prevAggregateTrade == null ) {
            prevAggregateTrade = currAggregateTrade;
            return historyPlayer.hasNext();
        }

        // 1)
        boolean priceUp = currAggregateTrade.getPrice().compareTo(prevAggregateTrade.getPrice()) > 0;
        // TODO check that isBuyerMaker corresponds to priceUp

//        Map<Long, List<MyOrder>> currentOrdersCopy = new HashMap<>(currentOrders);

        for (Map.Entry<Long, List<MyOrder>> currentOrdersEntry : currentOrders.entrySet()) {
            Iterator<MyOrder> clientOrderIt = new ArrayList<>(currentOrdersEntry.getValue()).iterator();
            while (clientOrderIt.hasNext()) {
//                System.out.println("Iteration Step: " + iterationStep);
//                if (iterationStep == 750290) {
//                    System.out.println("Exception on next step");
//                }

                MyOrder clientOrder = clientOrderIt.next();
                if (priceUp) {
                    if ((MyOrder.Type.LIMIT.equals(clientOrder.getType()) && MyOrder.Side.SELL.equals(clientOrder.getSide()))
                            || (MyOrder.Type.STOP_MARKET.equals(clientOrder.getType()) && MyOrder.Side.BUY.equals(clientOrder.getSide()))) {
                        if (prevAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) < 0 && currAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) >= 0) {
                            // order executed at clientOrder.getPrice()
                            fireOrderUpdate(currentOrdersEntry.getKey(), clientOrder, clientOrder.getPrice(), currAggregateTrade.getTime());
                        }
                    }
                } else { // price down
                    if ((MyOrder.Type.LIMIT.equals(clientOrder.getType()) && MyOrder.Side.BUY.equals(clientOrder.getSide()))
                            || (MyOrder.Type.STOP_MARKET.equals(clientOrder.getType()) && MyOrder.Side.SELL.equals(clientOrder.getSide()))) {
                        if (prevAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) > 0 && currAggregateTrade.getPrice().compareTo(clientOrder.getPrice()) <= 0) {
                            // order executed at clientOrder.getPrice()
                            fireOrderUpdate(currentOrdersEntry.getKey(), clientOrder, clientOrder.getPrice(), currAggregateTrade.getTime());
                        }
                    }
                }
                if (MyOrder.Type.MARKET.equals(clientOrder.getType())) {
                    // order executed at currAggregateTrade.getPrice()
                    fireOrderUpdate(currentOrdersEntry.getKey(), clientOrder, currAggregateTrade.getPrice(), currAggregateTrade.getTime());
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

        List<MyOrder> clientOrders = currentOrders.get(clientId);

        clientOrders.remove(clientOrder);
        OrderUpdateListener client = orderUpdateListeners.get(clientId);
        client.onOrderUpdate(clientOrderFilled);
    }

    private void fireNewPrice(AggregateTradeMini aggregateTrade) {
        for (PriceListener priceListener : priceListeners) {
            priceListener.onNewPrice(this.symbol, aggregateTrade.getPrice(), null);
        }
    }

    public void cancelOrdersBatchWithoutFire(Long clientId, List<MyOrder> ordersToCancel) {
        List<MyOrder> clientOrders = currentOrders.get(clientId);
        Iterator<MyOrder> clientOrdersIt = clientOrders.iterator();

        while(clientOrdersIt.hasNext()) { // TODO replace with #replaceIf
            if (ordersToCancel.contains(clientOrdersIt.next())) {
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
                System.out.println("prevAggregateTrade" + currAggregateTrade);
                System.out.println("Market Price (from prevAggregateTrade): " + currAggregateTrade.getPrice());
                System.out.println("Order Price : " + myOrder.getPrice());
                throw new IllegalArgumentException("Tried to place Limit Sell with price lower then Market price: iterationStep=" + iterationStep);
            }
        }

        List<MyOrder> clientOrders = currentOrders.get(clientId);
        if (clientOrders == null) {
            clientOrders = new ArrayList<>();
            currentOrders.put(clientId, clientOrders);
        }
        clientOrders.add(myOrder);
    }

    public void addPriceListener(PriceListener priceListener) {
        priceListeners.add(priceListener);
    }

    public void addOrderUpdateListener(Long clientId, OrderUpdateListener orderUpdateListener) {
        orderUpdateListeners.put(clientId, orderUpdateListener);
    }

    public void reset() {
        orderUpdateListeners.clear();
        priceListeners.clear();
        currentOrders.clear();
        iterationStep = 0;
        prevAggregateTrade = null;

        historyPlayer.rewind();
    }

}
