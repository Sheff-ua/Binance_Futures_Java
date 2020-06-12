package ua.dnepr.valera.crypto.bot.spot;

import com.binance.client.SyncRequestClient;
import com.binance.client.model.market.AggregateTrade;

import java.util.List;
import java.util.concurrent.Callable;

public class AggTradesSpotCallable implements Callable<List<AggregateTrade>> {

    private String symbol;
    private Long fromId;
    private SyncRequestClient syncRequestClient;

    public AggTradesSpotCallable(SyncRequestClient syncRequestClient, String symbol) {
        this.syncRequestClient = syncRequestClient;
        this.symbol = symbol;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    @Override
    public List<AggregateTrade> call() throws Exception {
        return this.syncRequestClient.getAggregateTradesSpot(symbol, fromId, null, null, 1000);
    }

}