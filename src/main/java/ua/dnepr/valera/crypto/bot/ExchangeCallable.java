package ua.dnepr.valera.crypto.bot;

import ua.dnepr.valera.crypto.bot.backtest.Exchange;
import ua.dnepr.valera.crypto.bot.backtest.StatisticsParamsDTO;

import java.util.List;
import java.util.concurrent.Callable;

public class ExchangeCallable implements Callable<List<StatisticsParamsDTO>> {

    private Exchange exchange;
    private int lastPercent = 0;

    public ExchangeCallable(Exchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public List<StatisticsParamsDTO> call() throws Exception {
        // Main work is in exchange.processNext()
        while (exchange.processNext()) {
            int currPercent = exchange.calcPercent();
            if (currPercent - lastPercent >= 1) {
                lastPercent = currPercent;
                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + String.format("  Current percent: %2d", lastPercent) + " %"); // FIXME uncomment for batch Bot3 mode
            }
        }

        return exchange.getResultingStatisticsList();
    }
}
