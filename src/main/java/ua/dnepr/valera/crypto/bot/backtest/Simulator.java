package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.ExchangeCallable;
import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.model.Bot3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

public class Simulator {

    private HashMap<Long, List<Long>> perfTest = new HashMap<>();
    private static int EXCHANGE_THREAD_COUNT = 6; // FIXME detect optimal value, 6, 10 looks good for now

    private static Long clientIdSequence = 1L;
    private static BigDecimal initialBalancePerBot = new BigDecimal("1000");

    private static List<StatisticsParamsDTO> statisticsParamsDTOList = new ArrayList<>();

    public static void main(String[] args) {

        long mainStart = System.currentTimeMillis();

        //String from = "2020-05-26"; String to = "2020-05-27"; String symbol = "BTCUSDT";
        //String from = "2020-06-07"; String to = "2020-06-08"; String symbol = "BTCUSDT";  // last painted case with average and variable take / stop
        //String from = "2019-10-01"; String to = "2019-11-30"; String symbol = "BTCUSDT";
        String from = "2020-02-01"; String to = "2020-03-01"; String symbol = "BTCUSDT"; // 1 month
        //String from = "2019-09-08"; String to = "2020-06-11"; String symbol = "BTCUSDT"; // all history


        HistoryStorage historyStorage = new HistoryStorage(symbol, true); // FIXME reduced history is used
        historyStorage.init(from, to);

        System.out.println("HistoryStorage created");

        /*============================================*/

        System.out.println("Start Simulation...");
        long simStart = System.currentTimeMillis();

        List<Bot3> bot3List = new ArrayList<>();
        // TODO this values should correspond to Strategy (for example average/last opening orders price should be less then stop price)
        BigDecimal fromTakeProfit = new BigDecimal("1"), toTakeProfit = new BigDecimal("20");
        BigDecimal fromStopLoss = new BigDecimal("3"), toStopLoss = new BigDecimal("10");
        BigDecimal stepTakeProfit = new BigDecimal("0.5"), stepStopLoss = new BigDecimal("0.5");

        BigDecimal takeProfit = fromTakeProfit;
        while (takeProfit.compareTo(toTakeProfit) <= 0) {
            BigDecimal stopLoss = fromStopLoss;
            while (stopLoss.compareTo(toStopLoss) <= 0) {
                Bot3 bot3 = new Bot3(clientIdSequence++, symbol, initialBalancePerBot);
                bot3.setTakeProfitPercent(takeProfit);
                bot3.setStopLossPercent(stopLoss);
                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + " Bot3 created. Take Profit: " + bot3.getTakeProfitPercent() + ", Stop Loss: " + bot3.getStopLossPercent());
//                if (!(new BigDecimal("3.50").compareTo(bot3.getTakeProfitPercent()) == 0 && new BigDecimal("5.00").compareTo(bot3.getStopLossPercent()) == 0)) {
//                    //Take Profit: 0.30, Stop Loss: 4.00
//                    continue;
//                }

                bot3List.add(bot3);

                stopLoss = stopLoss.add(stepStopLoss);
            }
            takeProfit = takeProfit.add(stepTakeProfit);
        }
        System.out.println("Total Bots created: " + bot3List.size());


        List<Exchange> exchangeList = new ArrayList<>();
        for (int i = 0; i < (bot3List.size() < EXCHANGE_THREAD_COUNT ? bot3List.size() : EXCHANGE_THREAD_COUNT); i++) {
            Exchange exchange = new Exchange(symbol);
            exchange.setHistoryStorage(historyStorage);
            exchangeList.add(exchange);
        }
        for (int i = 0; i < bot3List.size(); i++) {
            int exchangeIndex = i % EXCHANGE_THREAD_COUNT;
            Exchange exchange = exchangeList.get(exchangeIndex);
            Bot3 bot3 = bot3List.get(i);
            bot3.setExchange(exchange);
            exchange.addOrderUpdateListener(bot3.getClientId(), bot3);
            exchange.addPriceListener(bot3);
        }

        ExecutorService executor = Executors.newFixedThreadPool(EXCHANGE_THREAD_COUNT);
        List<Future<List<StatisticsParamsDTO>>> futures = new ArrayList<>();
        for (int i = 0; i < (bot3List.size() < EXCHANGE_THREAD_COUNT ? bot3List.size() : EXCHANGE_THREAD_COUNT); i++) {
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
        System.out.println();

        /*============================================*/

        Collections.sort(statisticsParamsDTOList);
        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
            System.out.println(statisticDTO.toBeautifulString());
        }
        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
            System.out.println(statisticDTO);
        }

        long mainEnd = System.currentTimeMillis();
        if (bot3List.size() == statisticsParamsDTOList.size()) {
            System.out.println("All {" + bot3List.size() + "} Simulations on ['" + from + "', '" + to + "'] period finished by thread pool of ( " + EXCHANGE_THREAD_COUNT + " ) threads in " + ((mainEnd - mainStart) / 1000) + " sec.");
        } else {
            System.out.println("Warning! Only  {"+ statisticsParamsDTOList.size() + "} Simulations on ['" + from + "', '" + to + "'] period finished by thread pool of ( " + EXCHANGE_THREAD_COUNT + " ) threads in " + ((mainEnd - mainStart) / 1000) +  " sec!!!");
        }
        executor.shutdown();

        BigDecimal sumBalance = BigDecimal.ZERO;
        for (Bot3 bot : bot3List) {
            sumBalance =  sumBalance.add(bot.getBalance());
        }
        BigDecimal sumBalanceDelta = BigDecimal.ZERO;
        for (Bot3 bot : bot3List) {
            sumBalanceDelta =  sumBalanceDelta.add(bot.getStatistics().balanceDelta());
        }
        System.out.println("Average Balance: " + Utils.formatPrice(sumBalance.divide(new BigDecimal(bot3List.size()), RoundingMode.DOWN))
                + ",  Average Balance Delta: " + Utils.formatPrice(sumBalanceDelta.divide(new BigDecimal(bot3List.size()), RoundingMode.DOWN)));
    }


}
