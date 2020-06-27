package ua.dnepr.valera.crypto.bot;

import ua.dnepr.valera.crypto.bot.backtest.Exchange;
import ua.dnepr.valera.crypto.bot.backtest.StatisticsParamsDTO;

import java.util.List;
import java.util.concurrent.Callable;

public class ExchangeCallable implements Callable<List<StatisticsParamsDTO>> {

    private Exchange exchange;
    private int id;
    private int lastPercent = 0;
    private ProgressLogger progressLogger;

    public ExchangeCallable(Exchange exchange, int id, ProgressLogger progressLogger) {
        this.exchange = exchange;
        this.id = id;
        this.progressLogger = progressLogger;
    }

    @Override
    public List<StatisticsParamsDTO> call() throws Exception {
        // Main work is in exchange.processNext()
        while (exchange.processNext()) {
            int currPercent = exchange.calcPercent();
            if (currPercent - lastPercent >= 1) {
                lastPercent = currPercent;
                progressLogger.onProgress(id, lastPercent);
                //System.out.print(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + String.format("  Current percent: %2d%% by Exchange %2d", lastPercent, id));
            }
        }

        return exchange.getResultingStatisticsList();
    }
}
