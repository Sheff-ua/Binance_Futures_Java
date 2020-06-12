package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.model.MyOrder;

import java.util.List;

public interface IExchange {

    public void placeOrder(Long clientId, MyOrder myOrder);

    public void cancelOrdersBatchWithoutFire(Long clientId, List<MyOrder> ordersToCancel);

}
