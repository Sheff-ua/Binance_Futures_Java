package ua.dnepr.valera.crypto.bot.backtest;

import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.model.Bot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Simulator {

    private static Long clientIdSequence = 1L;
    private static BigDecimal initialBalancePerBot = new BigDecimal("1000");

    private static List<StatisticsParamsDTO> statisticsParamsDTOList = new ArrayList<>();

    public static void main(String[] args) {

        long mainStart = System.currentTimeMillis();

        //String from = "2020-05-26"; String to = "2020-05-27"; String symbol = "BTCUSDT";
        //String from = "2019-10-01"; String to = "2019-11-30"; String symbol = "BTCUSDT";
        String from = "2019-09-09"; String to = "2020-06-09"; String symbol = "BTCUSDT";


        Exchange exchange = new Exchange(from, to, symbol);
        System.out.println("Exchange created");

        /*============================================*/

//        // while for for
//        BigDecimal initialTakeProfit = new BigDecimal("0.1");
//        BigDecimal initialStopLoss = new BigDecimal("0.2");
////        BigDecimal initialTakeProfit = new BigDecimal("0.5");
////        BigDecimal initialStopLoss = new BigDecimal("2");
//        int count = 0;
//        List<Bot> botList = new ArrayList<>();
//        for (int i = 0; i <= 100; i = i + 5) { // 10 gives 1.00 range // 100 gives 10.00 range
//            for (int j = 0; j <= 200; j = j + 10) { // 100 gives 10.00 range // 200 gives 20.00 range
//                // Bot created. Take Profit: 0.10, Stop Loss: 0.20
//                // Bot created. Take Profit: 1.10, Stop Loss: 10.20
//                count++;
//                Bot bot = new Bot(clientIdSequence++, exchange, symbol, initialBalancePerBot);
//                bot.setTakeProfitPercent(initialTakeProfit.add(new BigDecimal(i).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
//                bot.setStopLossPercent(initialStopLoss.add(new BigDecimal(j).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
//                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + " Bot created. Take Profit: " + bot.getTakeProfitPercent() + ", Stop Loss: " + bot.getStopLossPercent());
//
//                exchange.addOrderUpdateListener(bot.getClientId(), bot);
//                exchange.addPriceListener(bot);
//
//                botList.add(bot);
//            }
//        }
//
//        System.out.println("Start Simulation...");
//        long simStart = System.currentTimeMillis();
//
//        while (exchange.processNext()) {
//            // Main work is in exchange.processNext()
//        }
//
//        for (Bot bot : botList) {
//            statisticsParamsDTOList.add(new StatisticsParamsDTO(bot.getStatistics(), bot.getTakeProfitPercent(), bot.getStopLossPercent()));
//        }
//        long simEnd = System.currentTimeMillis();
//        System.out.println("Simulation finished in " + ((simEnd - simStart) / 1000) +  " sec.");
//        exchange.reset();

        /*============================================*/

        // for for while
        BigDecimal initialTakeProfit = new BigDecimal("0.1");
        BigDecimal initialStopLoss = new BigDecimal("0.2");
//        BigDecimal initialTakeProfit = new BigDecimal("0.5");
//        BigDecimal initialStopLoss = new BigDecimal("2");
        int count = 0;
        for (int i = 0; i <= 100; i = i + 5) { // 10 gives 1.00 range // 100 gives 10.00 range
            long takeProfitStart = System.currentTimeMillis();
            for (int j = 0; j <= 200; j = j+10) { // 100 gives 10.00 range // 200 gives 20.00 range
                // Bot created. Take Profit: 0.10, Stop Loss: 0.20
                // Bot created. Take Profit: 1.10, Stop Loss: 10.20
                count++;
                Bot bot = new Bot(clientIdSequence++, exchange, symbol, initialBalancePerBot);
                bot.setTakeProfitPercent(initialTakeProfit.add(new BigDecimal(i).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
                bot.setStopLossPercent(initialStopLoss.add(new BigDecimal(j).setScale(2, RoundingMode.DOWN).divide(new BigDecimal("10"), RoundingMode.DOWN))); // divide 10 gives 0.1 stepping
                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + " Bot created. Take Profit: " + bot.getTakeProfitPercent() + ", Stop Loss: " + bot.getStopLossPercent());

                exchange.addOrderUpdateListener(bot.getClientId(), bot);
                exchange.addPriceListener(bot);

                //System.out.println("Start Simulation...");
                long simStart = System.currentTimeMillis();

                while (exchange.processNext()) {
                    // Main work is in exchange.processNext()
                }

                //System.out.println(bot.getStatistics());
                //System.out.println("Balance: " + bot.getBalance());

//                System.out.println("Unclosed positions:");
//                for (MyPosition position : bot.getUnclosedPositions()) {
//                    System.out.println(position.getSide() + " Position {unRealizedPNL: " + position.calcUnRealizedPNL(exchange.getlastPrice()) + "}");
//                }

                statisticsParamsDTOList.add(new StatisticsParamsDTO(bot.getStatistics(), bot.getTakeProfitPercent(), bot.getStopLossPercent()));
                long simEnd = System.currentTimeMillis();
                System.out.println("Simulation finished in " + ((simEnd - simStart) / 1000) +  " sec.");
                exchange.reset();
            }
            long takeProfitEnd = System.currentTimeMillis();
            System.out.println("Single Take profit Simulation finished in " + ((takeProfitEnd - takeProfitStart) / 1000) +  " sec.");
        }


        /*============================================*/

        Collections.sort(statisticsParamsDTOList);
        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
            System.out.println(statisticDTO);
        }

        long mainEnd = System.currentTimeMillis();
        System.out.println("All {"+ count + "} Simulations finished in " + ((mainEnd - mainStart) / 1000) +  " sec.");

//        try {
//            Thread.sleep(60 * 60 * 1000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


}
