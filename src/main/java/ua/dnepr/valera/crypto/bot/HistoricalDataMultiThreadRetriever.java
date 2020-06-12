package ua.dnepr.valera.crypto.bot;

import com.binance.client.*;
import com.binance.client.model.market.AggregateTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Deprecated
public class HistoricalDataMultiThreadRetriever {
    private static final Logger log = LoggerFactory.getLogger(HistoricalDataMultiThreadRetriever.class);

    //private static String SYMBOL = "LINKUSDT";
    private static int THREAD_COUNT = 20;

    //private static final DateTimeFormatter formatter8 = java.time.format.DateTimeFormatter.ofPattern("YYYY.MM.dd HH:mm:ss.SSS V");
    private static final DateTimeFormatter formatter8 = DateTimeFormatter.ISO_INSTANT;


    private static String listenKey;
    private static SyncRequestClient syncRequestClient;


    private static final String FILE_HEADER = "id,price,qty,firstId,lastId,time,isBuyerMaker";
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";


    public static synchronized String formatDate8(long timeInMillis) {
        return formatter8.format(Instant.ofEpochMilli(timeInMillis).atZone(ZoneOffset.UTC));
    }

    public static synchronized ZonedDateTime parseDate8(String dateString) {
        return Instant.parse(dateString).atZone(ZoneOffset.UTC);
    }

    private static String getFileName(String symbol, AggregateTrade aggTrade, boolean unclosed) {
        // 0000000000 94141510
        // 0000000000 9414
        String stub = "0000000000";
        String temp = stub + String.valueOf(aggTrade.getId());
        String datePart = formatDate8(aggTrade.getTime());
        //2019-09-08T23:59:58.684Z
        return "history-" + symbol.toLowerCase() + "-" + temp.substring(temp.length()-10, temp.length()) + "_" + datePart.substring(0, 10)
                + (unclosed ? "-unclosed" : "");
    }


    private static void writeCSV(String fileName, List<AggregateTrade> aggregateTrades) {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(fileName);

            //Write the CSV file header
            fileWriter.append(FILE_HEADER.toString());

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
                fileWriter.append(aggregateTrade.getFirstId().toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.getLastId().toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.getTime().toString());
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(formatDate8(aggregateTrade.getTime()));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(aggregateTrade.isBuyerMaker().toString());

                fileWriter.append(NEW_LINE_SEPARATOR);
            }



            System.out.println("CSV file was created successfully !!!");

        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
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


    public static void main(String[] args) {

        /****** UI *********/
        //Creating the Frame
        JFrame frame = new JFrame("HistoricalDataSpotMultiThreadRetriever");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1320, 350);
        //frame.setLocation(1715, 300); // right center part
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


        // TODO find out first day / hour/ minute from which the aggregated trades are available !!!


        //String[] symbols = {"BCHUSDT","LTCUSDT","TRXUSDT","ADAUSDT","XMRUSDT","XLMUSDT","DASHUSDT","ZECUSDT","XTZUSDT","ATOMUSDT","ONTUSDT","IOTAUSDT","BATUSDT","VETUSDT","NEOUSDT","IOSTUSDT"};
        String[] symbols = {"BTCUSDT"};

        for (String symbol : symbols) {
            RequestOptions options = new RequestOptions();
            syncRequestClient = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, options);

            List<AggTradesCallable> aggTradesCallables = new ArrayList<>();
            for (int i=1; i<=THREAD_COUNT; i++) {
                RequestOptions o = new RequestOptions();
                SyncRequestClient syncRC = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, o);
                AggTradesCallable aggTradesCallable = new AggTradesCallable(syncRC, symbol);
                aggTradesCallables.add(aggTradesCallable);
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

            long id = 96522253;
            List<AggregateTrade> aggTradesInit = syncRequestClient.getAggregateTrades(symbol, id, null, null, 1);
            AggregateTrade prevAggTrade = aggTradesInit.get(0);
            int lastDay = Instant.ofEpochMilli(prevAggTrade.getTime()).atZone(ZoneOffset.UTC).getDayOfMonth();
            id = prevAggTrade.getId(); // if history starts from 3 like in BTC

            List<AggregateTrade> oneDayAggregateTradeList = new ArrayList<>();

            int prevMinute = Instant.now().atZone(ZoneId.systemDefault()).getMinute();
            int callCounts = 0;

            while (true) {
                /** Rate limitation */
                int currMinute = Instant.now().atZone(ZoneId.systemDefault()).getMinute();
                if (currMinute == prevMinute) {
                    callCounts = callCounts + THREAD_COUNT;
                    log.info("1) currMinute: " + currMinute + " , callCounts: " + callCounts);
                }
                if (currMinute != prevMinute) {
                    prevMinute = currMinute;
                    callCounts = THREAD_COUNT;
                    log.info("2) currMinute: " + currMinute + " , callCounts: " + callCounts);
                }
                if (callCounts >= 2360) { // TODO get from Exchange info
                    int currSecond = Instant.now().atZone(ZoneId.systemDefault()).getSecond();
                    log.info("Rate limit almost exceeded. Sleeping for: " + ((60 - currSecond +2) * 1000L) + "mSec");
                    log.info("3) currMinute: " + currMinute + " , callCounts: " + callCounts);
                    try {
                        Thread.sleep((60 - currSecond + 2) * 1000L);
                    }catch (InterruptedException e) {
                        log.error(e.toString());
                        e.printStackTrace();
                    }
                }

                /** Main */
                List<Future<List<AggregateTrade>>> futures = new ArrayList<>();
                for (int i = 1; i <= THREAD_COUNT; i++) {
                    AggTradesCallable aggTradesCallable = aggTradesCallables.get(i - 1);
                    aggTradesCallable.setFromId(id);
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

                        log.info("writing csv: " + getFileName(symbol, prevAggTrade, false));
                        writeCSV(getFileName(symbol, prevAggTrade, false), oneDayAggregateTradeList);

                        oneDayAggregateTradeList.clear();

                        lastDay = currDay;
                    }

                    oneDayAggregateTradeList.add(aggTrade);
                    prevAggTrade = aggTrade;
                }

                long lastId = iterationAggregateTradeList.get(iterationAggregateTradeList.size()-1).getId();

                if (iterationAggregateTradeList.size() < THREAD_COUNT * 1000) {
                    log.info("Stopping!!!! lastId: " + lastId + ", id: " + id);
                    log.info("writing csv: " + getFileName(symbol, prevAggTrade, true));
                    writeCSV(getFileName(symbol, prevAggTrade,true), oneDayAggregateTradeList);
                    break;
                }

                id = lastId + 1;

            }

            try {
                Thread.sleep((63) * 1000L);
            }catch (InterruptedException e) {
                log.error(e.toString());
                e.printStackTrace();
            }
        }

    }

}
