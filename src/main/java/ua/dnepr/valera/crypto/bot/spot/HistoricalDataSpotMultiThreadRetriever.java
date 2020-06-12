package ua.dnepr.valera.crypto.bot.spot;

import com.binance.client.*;
import com.binance.client.model.market.AggregateTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.dnepr.valera.crypto.bot.AggTradesCallable;
import ua.dnepr.valera.crypto.bot.PrivateConfig;
import ua.dnepr.valera.crypto.bot.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class HistoricalDataSpotMultiThreadRetriever {
    private static final Logger log = LoggerFactory.getLogger(HistoricalDataSpotMultiThreadRetriever.class);

    private static int THREAD_COUNT_SPOT = 10;
    private static int THREAD_COUNT_FEATUERES = 20;
    private static int CALL_RATE_LIMIT_SPOT = 1180; // 1200
    private static int CALL_RATE_LIMIT_FEATURES = 2360; // 2400
    private static String DIRECTORY = "c:\\Projects\\Crypto\\production_history\\";

    private static final String FILE_HEADER = "id,price,qty,time,isBuyerMaker";
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";

    private static final BigDecimal REDUCE_PERCENT = new BigDecimal("0.005");

    private static final DateTimeFormatter formatter8 = DateTimeFormatter.ISO_INSTANT;

    private static SyncRequestClient syncRequestClient;


    public static synchronized String formatDate8(long timeInMillis) {
        return formatter8.format(Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC));
    }

    private static String getFileName(boolean isSpot, boolean isReduced, String symbol, AggregateTrade aggTrade, boolean unclosed) {
        String stub = "0000000000";
        String temp = stub + aggTrade.getId();
        String datePart = formatDate8(aggTrade.getTime());
        return DIRECTORY + symbol.toUpperCase() + (isSpot ? "-spot" : "") + (isReduced ? "-reduced" : "") + "\\" + "history-" + symbol.toLowerCase() + "-" + temp.substring(temp.length()-10) + "_" + datePart.substring(0, 10) + (unclosed ? "-unclosed" : "");
    }

    private static void writeCSV(String fileName, List<AggregateTrade> aggregateTrades) {
        FileWriter fileWriter = null;

        try {

            fileWriter = new FileWriter(fileName);

            //Write the CSV file header
            fileWriter.append(FILE_HEADER);

            //Add a new line separator after the header
            fileWriter.append(NEW_LINE_SEPARATOR);

            //Write a new student object list to the CSV file
            for (AggregateTrade aggregateTrade : aggregateTrades) {
                fileWriter.append(aggregateTrade.getId().toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.getPrice().toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.getQty().toString());
                fileWriter.append(COMMA_DELIMITER);
//                fileWriter.append(aggregateTrade.getFirstId().toString());
//                fileWriter.append(COMMA_DELIMITER);
//                fileWriter.append(aggregateTrade.getLastId().toString());
//                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.getTime().toString());
                fileWriter.append(COMMA_DELIMITER);
//                fileWriter.append(formatDate8(aggregateTrade.getTime()));
//                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.isBuyerMaker().toString());

                fileWriter.append(NEW_LINE_SEPARATOR);
            }
        } catch (Exception e) {
            log.error("Error during CSV-file write");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                log.error("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }

    private static List<AggregateTrade> reduce(List<AggregateTrade> oneDayAggregateTradeList) {
        List<AggregateTrade> reducedList = new LinkedList<>();
        AggregateTrade lastWrittenAggregateTrade = null;
        for (AggregateTrade trade : oneDayAggregateTradeList) {
            if (lastWrittenAggregateTrade == null) {
                lastWrittenAggregateTrade = trade;
                reducedList.add(trade);
                continue;
            }

            if (trade.getPrice().compareTo(lastWrittenAggregateTrade.getPrice()) > 0) { // next price is greater
                // if trade > lastWrittenAggregateTrade + percent of lastWrittenAggregateTrade
                if (trade.getPrice().compareTo(lastWrittenAggregateTrade.getPrice().add(Utils.calcXPercentsFromY(REDUCE_PERCENT, lastWrittenAggregateTrade.getPrice()))) > 0) {
                    lastWrittenAggregateTrade = trade;
                    reducedList.add(trade);
                }
            } else { // next price is less
                // if trade < lastWrittenAggregateTrade - percent of lastWrittenAggregateTrade
                if (trade.getPrice().compareTo(lastWrittenAggregateTrade.getPrice().subtract(Utils.calcXPercentsFromY(REDUCE_PERCENT, lastWrittenAggregateTrade.getPrice()))) < 0) {
                    lastWrittenAggregateTrade = trade;
                    reducedList.add(trade);
                }
            }
        }

        //Сколько процентов составляет 24 от числа 248 ?
        //Итог - 9.677 %
        //Как вычислять:
        //Получаем коэффициент - 248 / 24 = 10.333
        //Получаем проценты - 100% / 10.333 = 9.677 %

        BigDecimal coeff = new BigDecimal(oneDayAggregateTradeList.size()).divide(new BigDecimal(reducedList.size()), 2, RoundingMode.DOWN);
        BigDecimal reducedByPercents = new BigDecimal("100").subtract(new BigDecimal("100").divide(coeff, 2, RoundingMode.DOWN));
        System.out.println("oneDayAggregateTradeList is reduced by " + reducedByPercents + "%");

        return reducedList;
    }

    public static void main(String[] args) {

        boolean isSpot = false;

        /****** UI *********/
        //Creating the Frame
        JFrame frame = new JFrame("HistoricalDataSpotMultiThreadRetriever");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1320, 350);
        frame.setLocation(0, 351); // left up part


        JButton applyChanges = new JButton("Apply");

        JPanel upsidePanel = new JPanel();
        GridLayout upsideGridLayout = new GridLayout(2, 1);
        upsidePanel.setLayout(upsideGridLayout);

        frame.getContentPane().add(BorderLayout.NORTH, upsidePanel);

        upsidePanel.add(applyChanges);
        frame.setVisible(true);

        applyChanges.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                log.info(" Apply clicked !!!");

            }
        });

        //String[] symbols = {"ETHBTC", "GRSBTC", "EOSBTC", "BNBBTC"};
        //String[] symbols = {"BCHUSDT","LTCUSDT","TRXUSDT","ADAUSDT","XMRUSDT","XLMUSDT","DASHUSDT","ZECUSDT","XTZUSDT","ATOMUSDT","ONTUSDT","IOTAUSDT","BATUSDT","VETUSDT","NEOUSDT","IOSTUSDT"};
        String[] symbols = {"BTCUSDT"};
        int proceedId = 1; // TODO set to 1 if no need

        for (String symbol : symbols) {
            RequestOptions options = new RequestOptions();
            syncRequestClient = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, options);

            List<Callable> aggTradesCallables = new ArrayList<>();
            for (int i = 1; i <= (isSpot ? THREAD_COUNT_SPOT : THREAD_COUNT_FEATUERES); i++) {
                RequestOptions o = new RequestOptions();
                SyncRequestClient syncRC = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, o);
                Callable aggTradesCallable = null;
                if (isSpot) {
                    aggTradesCallable = new AggTradesSpotCallable(syncRC, symbol);
                } else {
                    aggTradesCallable = new AggTradesCallable(syncRC, symbol);
                }
                aggTradesCallables.add(aggTradesCallable);
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT_SPOT);


            long id = 1;
            if (proceedId > 1) {
                id = proceedId;
                proceedId = 1;
            }
            List<AggregateTrade> aggTradesInit = null;
            if (isSpot) {
                aggTradesInit = syncRequestClient.getAggregateTradesSpot(symbol, id, null, null, 1);
            } else {
                aggTradesInit = syncRequestClient.getAggregateTrades(symbol, id, null, null, 1);
            }
            AggregateTrade prevAggTrade = aggTradesInit.get(0);
            int lastDay = Instant.ofEpochMilli(prevAggTrade.getTime()).atZone(ZoneOffset.UTC).getDayOfMonth();
            id = prevAggTrade.getId(); // if history starts from 3 like in BTC

            List<AggregateTrade> aggTradesLastKnown = null;
            if (isSpot) {
                aggTradesLastKnown = syncRequestClient.getAggregateTradesSpot(symbol, null, null, null, 1);
            } else {
                aggTradesLastKnown = syncRequestClient.getAggregateTrades(symbol, null, null, null, 1);
            }
            long lastKnownId = aggTradesLastKnown.get(0).getId();


            List<AggregateTrade> oneDayAggregateTradeList = new ArrayList<>();

            int prevMinute = Instant.now().atZone(ZoneId.systemDefault()).getMinute();
            int callCounts = 0;

            while (true) {
                /** Rate limitation */
                int currMinute = Instant.now().atZone(ZoneId.systemDefault()).getMinute();
                if (currMinute == prevMinute) {
                    callCounts = callCounts + THREAD_COUNT_SPOT;
                    log.debug("1) currMinute: " + currMinute + " , callCounts: " + callCounts);
                }
                if (currMinute != prevMinute) {
                    prevMinute = currMinute;
                    callCounts = THREAD_COUNT_SPOT;
                    log.debug("2) currMinute: " + currMinute + " , callCounts: " + callCounts);
                }
                if (callCounts >= (isSpot ? CALL_RATE_LIMIT_SPOT : CALL_RATE_LIMIT_FEATURES)) { // TODO get from Exchange info
                    int currSecond = Instant.now().atZone(ZoneId.systemDefault()).getSecond();
                    log.info("Rate limit (" + (isSpot ? CALL_RATE_LIMIT_SPOT : CALL_RATE_LIMIT_FEATURES) + ") almost exceeded. Sleeping for: " + (60 - currSecond +2) + " Sec");
                    log.debug("3) currMinute: " + currMinute + " , callCounts: " + callCounts);
                    try {
                        Thread.sleep((60 - currSecond + 2) * 1000L);
                    }catch (InterruptedException e) {
                        log.error(e.toString());
                        e.printStackTrace();
                    }
                }

                /** Main */
                List<Future<List<AggregateTrade>>> futures = new ArrayList<>();
                for (int i = 1; i <= (isSpot ? THREAD_COUNT_SPOT : THREAD_COUNT_FEATUERES); i++) {
                    Callable aggTradesCallable = aggTradesCallables.get(i - 1);
                    if (isSpot) {
                        ((AggTradesSpotCallable)aggTradesCallable).setFromId(id);
                    } else {
                        ((AggTradesCallable)aggTradesCallable).setFromId(id);
                    }
                    Future<List<AggregateTrade>> future = executor.submit(aggTradesCallable);
                    futures.add(future);
                    id = id + 1000;
                }

                List<AggregateTrade> iterationAggregateTradeList = new ArrayList<>();
                for (Future<List<AggregateTrade>> future : futures) {
                    try {
                        iterationAggregateTradeList.addAll(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                Collections.sort(iterationAggregateTradeList, new Comparator<AggregateTrade>() {
                    @Override
                    public int compare(AggregateTrade o1, AggregateTrade o2) {
                        return o1.getId().compareTo(o2.getId());
                    }
                });

                for (AggregateTrade aggTrade : iterationAggregateTradeList) {
                    int currDay = Instant.ofEpochMilli(aggTrade.getTime()).atZone(ZoneOffset.UTC).getDayOfMonth();

                    if (lastDay != currDay) {
                        log.info("Writing csv: " + getFileName(isSpot, false, symbol, prevAggTrade, false));
                        writeCSV(getFileName(isSpot, false, symbol, prevAggTrade, false), oneDayAggregateTradeList);

                        List<AggregateTrade> oneDayAggregateTradeReducedList = reduce(oneDayAggregateTradeList);
                        log.info("Writing csv: " + getFileName(isSpot, true, symbol, prevAggTrade, false));
                        writeCSV(getFileName(isSpot, true, symbol, prevAggTrade, false), oneDayAggregateTradeReducedList);

                        double currPercent = id * 1.0 / lastKnownId * 100;
                        log.info(String.format("Current percent: %2.2f", currPercent) + " %");

                        oneDayAggregateTradeList.clear();

                        lastDay = currDay;
                    }

                    oneDayAggregateTradeList.add(aggTrade);
                    prevAggTrade = aggTrade;
                }

                long lastId = iterationAggregateTradeList.get(iterationAggregateTradeList.size()-1).getId();

                if (iterationAggregateTradeList.size() < (isSpot ? THREAD_COUNT_SPOT : THREAD_COUNT_FEATUERES) * 1000) {
                    log.info("Stopping!!!! lastId: " + lastId + ", id: " + id);
                    log.info("writing csv: " + getFileName(isSpot, false, symbol, prevAggTrade, true));
                    writeCSV(getFileName(isSpot, false, symbol, prevAggTrade,true), oneDayAggregateTradeList);

                    List<AggregateTrade> oneDayAggregateTradeReducedList = reduce(oneDayAggregateTradeList);
                    log.info("Writing csv: " + getFileName(isSpot, true, symbol, prevAggTrade, false));
                    writeCSV(getFileName(isSpot, true, symbol, prevAggTrade, false), oneDayAggregateTradeReducedList);

                    break;
                }

                id = lastId + 1;
            }

            try {
                System.out.println("Sleeping for 63 sec between Symbols");
                Thread.sleep((63) * 1000L);
            }catch (InterruptedException e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }

    }

}
