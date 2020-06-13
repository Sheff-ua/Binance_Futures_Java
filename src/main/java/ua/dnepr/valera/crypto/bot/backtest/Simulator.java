package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.ExchangeCallable;
import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.model.Bot2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

public class Simulator {

    private HashMap<Long, List<Long>> perfTest = new HashMap<>();
    private static int EXCHANGE_THREAD_COUNT = 1; // FIXME detect optimal value, 10 looks good for now

    private static Long clientIdSequence = 1L;
    private static BigDecimal initialBalancePerBot = new BigDecimal("1000");

    private static List<StatisticsParamsDTO> statisticsParamsDTOList = new ArrayList<>();

    public static void main(String[] args) {

        long mainStart = System.currentTimeMillis();

        //String from = "2020-05-26"; String to = "2020-05-27"; String symbol = "BTCUSDT";
        //String from = "2019-10-01"; String to = "2019-11-30"; String symbol = "BTCUSDT";
        String from = "2019-09-09"; String to = "2020-06-09"; String symbol = "BTCUSDT";


        HistoryStorage historyStorage = new HistoryStorage(symbol, true); // FIXME reduced history is used
        historyStorage.init(from, to);

        System.out.println("HistoryStorage created");

        /*============================================*/

        System.out.println("Start Simulation...");
        long simStart = System.currentTimeMillis();

        List<Bot2> bot2List = new ArrayList<>();
        BigDecimal initialTakeProfit = new BigDecimal("0.3");
        BigDecimal initialStopLoss = new BigDecimal("2"); // TODO from which value ?
        for (int i = 0; i <= 100; i = i + 5) { // 10 gives 1.00 range // 100 gives 10.00 range
            for (int j = 0; j <= 200; j = j + 10) { // 100 gives 10.00 range // 200 gives 20.00 range
                Bot2 bot2 = new Bot2(clientIdSequence++, symbol, initialBalancePerBot);
                bot2.setTakeProfitPercent(initialTakeProfit.add(new BigDecimal(i).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
                bot2.setStopLossPercent(initialStopLoss.add(new BigDecimal(j).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + " Bot2 created. Take Profit: " + bot2.getTakeProfitPercent() + ", Stop Loss: " + bot2.getStopLossPercent());
                bot2List.add(bot2);
            }
        }

        List<Exchange> exchangeList = new ArrayList<>();
        for (int i = 0; i < EXCHANGE_THREAD_COUNT; i++) {
            Exchange exchange = new Exchange(symbol);
            exchange.setHistoryStorage(historyStorage);
            exchangeList.add(exchange);
        }
        for (int i = 0; i < bot2List.size(); i++) {
            int exchangeIndex = i % EXCHANGE_THREAD_COUNT;
            Exchange exchange = exchangeList.get(exchangeIndex);
            Bot2 bot2 = bot2List.get(i);
            bot2.setExchange(exchange);
            exchange.addOrderUpdateListener(bot2.getClientId(), bot2);
            exchange.addPriceListener(bot2);
            // FIXME check that one bot2 doesn't belong to 2 or more exchanges !!!
        }

        ExecutorService executor = Executors.newFixedThreadPool(EXCHANGE_THREAD_COUNT);
        List<Future<List<StatisticsParamsDTO>>> futures = new ArrayList<>();
        for (int i = 0; i < EXCHANGE_THREAD_COUNT; i++) {
            Future<List<StatisticsParamsDTO>> future = executor.submit(new ExchangeCallable(exchangeList.get(i)));
            futures.add(future);
        }

        for (Future<List<StatisticsParamsDTO>> future : futures) {
            try {
                statisticsParamsDTOList.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }



        long simEnd = System.currentTimeMillis();
        System.out.println("Simulation finished in " + ((simEnd - simStart) / 1000) +  " sec.");


        /*============================================*/

        Collections.sort(statisticsParamsDTOList);
        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
            System.out.println(statisticDTO);
        }

        long mainEnd = System.currentTimeMillis();
        System.out.println("All {"+ bot2List.size() + "} Simulations on ['" + from + "', '" + to + "'] period finished by thread pool of ( " + EXCHANGE_THREAD_COUNT + " ) threads in " + ((mainEnd - mainStart) / 1000) +  " sec.");
        executor.shutdown();
    }


}
