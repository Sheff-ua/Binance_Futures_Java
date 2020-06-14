package ua.dnepr.valera.crypto.bot.model;

import com.binance.client.model.market.AggregateTrade;

import java.math.BigDecimal;

public interface PriceListener {

    void onNewPrice(String symbol, BigDecimal price, long time, Boolean isSell);

}
