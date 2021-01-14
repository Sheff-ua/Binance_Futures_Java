package ua.dnepr.valera.crypto.bot.backtest;

import com.binance.client.model.market.AggregateTrade;
import ua.dnepr.valera.crypto.bot.ExchangeCallable;
import ua.dnepr.valera.crypto.bot.ProgressLogger;
import ua.dnepr.valera.crypto.bot.Utils;
import ua.dnepr.valera.crypto.bot.model.Bot1b;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

import static ua.dnepr.valera.crypto.bot.Utils.NEW_LINE_SEPARATOR;

public class Simulator {

    private static String DIRECTORY = "c:\\Projects\\Crypto\\backtest_history\\";
    private HashMap<Long, List<Long>> perfTest = new HashMap<>();
    private static int EXCHANGE_THREAD_COUNT = 8; // FIXME detect optimal value, 6, 10 looks good for now

    private static Long clientIdSequence = 1L;
    private static BigDecimal initialBalancePerBot = new BigDecimal("1000");

    private static List<StatisticsParamsDTO> statisticsParamsDTOList = new ArrayList<>();

    public static void main(String[] args) {

        long mainStart = System.currentTimeMillis();

        //String from = "2020-05-27"; String to = "2020-05-27"; String symbol = "BTCUSDT"; // 1 day
        //String from = "2020-05-26"; String to = "2020-05-27"; String symbol = "BTCUSDT"; // 2 days
        String from = "2020-05-26"; String to = "2020-05-28"; String symbol = "BTCUSDT"; // 3 days
        //String from = "2019-10-01"; String to = "2019-11-30"; String symbol = "BTCUSDT";  // 2 months
        //String from = "2020-02-01"; String to = "2020-03-01"; String symbol = "BTCUSDT"; // 1 month
        //String from = "2019-09-08"; String to = "2020-06-11"; String symbol = "BTCUSDT"; // all history


        HistoryStorage historyStorage = new HistoryStorage(symbol, true); // FIXME reduced history is used
        historyStorage.init(from, to);

        System.out.println("HistoryStorage created");

        /*============================================*/

        System.out.println("Start Simulation...");
        long simStart = System.currentTimeMillis();

        //List<Bot3Fibo> botList = new ArrayList<>();
        List<Bot1b> botList = new ArrayList<>();
        // TODO this values should correspond to Strategy (for example average/last opening orders price should be less then stop price)
//        BigDecimal fromTakeProfit = new BigDecimal("0.1"), toTakeProfit = new BigDecimal("0.5"); // Fibo
//        BigDecimal fromStopLoss = new BigDecimal("1"), toStopLoss = new BigDecimal("4");
//        BigDecimal stepTakeProfit = new BigDecimal("0.05"), stepStopLoss = new BigDecimal("0.5");

        BigDecimal fromTakeProfit = new BigDecimal("1.5"), toTakeProfit = new BigDecimal("10"); // Bot1
        BigDecimal fromStopLoss = new BigDecimal("1.5"), toStopLoss = new BigDecimal("10");
        BigDecimal stepTakeProfit = new BigDecimal("0.5"), stepStopLoss = new BigDecimal("0.5");

        BigDecimal takeProfit = fromTakeProfit;
        while (takeProfit.compareTo(toTakeProfit) <= 0) {
            BigDecimal stopLoss = fromStopLoss;
            while (stopLoss.compareTo(toStopLoss) <= 0) {
                Bot1b bot = new Bot1b(clientIdSequence++, symbol, initialBalancePerBot);
                bot.setTakeProfitPercent(takeProfit);
                bot.setStopLossPercent(stopLoss);
                System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + " Bot created. Take Profit: " + bot.getTakeProfitPercent() + ", Stop Loss: " + bot.getStopLossPercent());
//                if (!(new BigDecimal("3.50").compareTo(bot.getTakeProfitPercent()) == 0 && new BigDecimal("5.00").compareTo(bot.getStopLossPercent()) == 0)) {
//                    //Take Profit: 0.30, Stop Loss: 4.00
//                    continue;
//                }

                botList.add(bot);

                stopLoss = stopLoss.add(stepStopLoss);
            }
            takeProfit = takeProfit.add(stepTakeProfit);
        }
        System.out.println("Total Bots created: " + botList.size());
        System.out.println(" ");


        List<Exchange> exchangeList = new ArrayList<>();
        for (int i = 0; i < (botList.size() < EXCHANGE_THREAD_COUNT ? botList.size() : EXCHANGE_THREAD_COUNT); i++) {
            Exchange exchange = new Exchange(symbol);
            exchange.setHistoryStorage(historyStorage);
            exchangeList.add(exchange);
        }
        for (int i = 0; i < botList.size(); i++) {
            int exchangeIndex = i % EXCHANGE_THREAD_COUNT;
            Exchange exchange = exchangeList.get(exchangeIndex);
            Bot1b bot = botList.get(i);
            bot.setExchange(exchange);
            exchange.addOrderUpdateListener(bot.getClientId(), bot);
            exchange.addPriceListener(bot);
        }

        ProgressLogger progressLogger = new ProgressLogger(System.currentTimeMillis());
        ExecutorService executor = Executors.newFixedThreadPool(EXCHANGE_THREAD_COUNT);
        List<Future<List<StatisticsParamsDTO>>> futures = new ArrayList<>();
        for (int i = 0; i < (botList.size() < EXCHANGE_THREAD_COUNT ? botList.size() : EXCHANGE_THREAD_COUNT); i++) {
            Future<List<StatisticsParamsDTO>> future = executor.submit(new ExchangeCallable(exchangeList.get(i), i, progressLogger));
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
        System.out.println(" ");System.out.println(" ");
        System.out.println("Simulation finished in " + ((simEnd - simStart) / 1000) +  " sec.");
        System.out.println();

        /*============================================*/

        Collections.sort(statisticsParamsDTOList);
//        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
//            System.out.println(statisticDTO.toBeautifulString());
//        }
        for (StatisticsParamsDTO statisticDTO : statisticsParamsDTOList) {
            System.out.println(statisticDTO);
        }

        // TODO write CSV
        writeCSV(getFileName(botList.get(0).getClass(), from, to), statisticsParamsDTOList);

        long mainEnd = System.currentTimeMillis();
        if (botList.size() == statisticsParamsDTOList.size()) {
            System.out.println("All {" + botList.size() + "} Simulations on ['" + from + "', '" + to + "'] period finished by thread pool of ( " + EXCHANGE_THREAD_COUNT + " ) threads in " + ((mainEnd - mainStart) / 1000) + " sec.");
        } else {
            System.out.println("Warning! Only  {"+ statisticsParamsDTOList.size() + "} Simulations on ['" + from + "', '" + to + "'] period finished by thread pool of ( " + EXCHANGE_THREAD_COUNT + " ) threads in " + ((mainEnd - mainStart) / 1000) +  " sec!!!");
        }
        System.out.println("Bot Class: " + botList.get(0).getClass().getSimpleName());
        System.out.println("Bot Description: " + botList.get(0).getDescription());
        executor.shutdown();

        BigDecimal sumBalance = BigDecimal.ZERO;
        for (Bot1b bot : botList) {
            sumBalance =  sumBalance.add(bot.getBalance());
        }
        BigDecimal sumBalanceDelta = BigDecimal.ZERO;
        for (Bot1b bot : botList) {
            sumBalanceDelta =  sumBalanceDelta.add(bot.getStatistics().balanceDelta());
        }

        System.out.println("Average Balance: " + Utils.formatPrice(sumBalance.divide(new BigDecimal(botList.size()), RoundingMode.DOWN))
                + ",  Average Balance Delta: " + Utils.formatPrice(sumBalanceDelta.divide(new BigDecimal(botList.size()), RoundingMode.DOWN)));
    }

    private static String getFileName(Class botClass, String from, String to) {
        return DIRECTORY + "\\"  + botClass.getSimpleName()+ "_" + from + "_" + to + ".csv";
    }

    private static void writeCSV(String fileName, List<StatisticsParamsDTO> statisticsParamsDTOList) {
        FileWriter fileWriter = null;

        try {

            fileWriter = new FileWriter(fileName);

            //Write the CSV file header
            fileWriter.append(StatisticsParamsDTO.toCSVHeader());

            //Add a new line separator after the header
            fileWriter.append(NEW_LINE_SEPARATOR);

            //Write a new student object list to the CSV file
            for (StatisticsParamsDTO statisticsParamsDTO : statisticsParamsDTOList) {
                fileWriter.append(statisticsParamsDTO.toCSV());

                fileWriter.append(NEW_LINE_SEPARATOR);
            }
        } catch (Exception e) {
            System.out.println("Error during CSV-file write");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }

}
