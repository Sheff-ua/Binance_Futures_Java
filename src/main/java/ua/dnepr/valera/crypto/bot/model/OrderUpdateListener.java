package ua.dnepr.valera.crypto.bot.model;

import ua.dnepr.valera.crypto.bot.backtest.StatisticsParamsDTO;

public interface OrderUpdateListener {

    void onOrderUpdate(MyOrder updatedOrder);

    StatisticsParamsDTO getStatisticsParamsDTO();

}
