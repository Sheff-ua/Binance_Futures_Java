package ua.dnepr.valera.crypto.bot;

import com.binance.client.*;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.market.AggregateTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoricalDataSingleThreadRetriever {
    private static final Logger log = LoggerFactory.getLogger(HistoricalDataSingleThreadRetriever.class);

    private static String SYMBOL = "QTUMUSDT";

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

    private static String getFileName(AggregateTrade aggTrade) {
        // 0000000000 94141510
        // 0000000000 9414
        String stub = "0000000000";
        String temp = stub + String.valueOf(aggTrade.getId());
        String datePart = formatDate8(aggTrade.getTime());
        //2019-09-08T23:59:58.684Z
        return "history-" + SYMBOL.toLowerCase() + "-" + temp.substring(temp.length()-10, temp.length()) + "_" + datePart.substring(0, 10);
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
        JFrame frame = new JFrame("HistoricalDataSingleThreadRetriever");
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
            int l = 10;
            public void actionPerformed(ActionEvent e) {
                log.info(" Apply clicked !!!");

            }
        });


        // TODO find out first day / hour/ minute from which the aggregated trades are available !!!



        RequestOptions options = new RequestOptions();
        syncRequestClient = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, options);


        try {
            long id = 1;
            List<AggregateTrade> aggTradesInit = syncRequestClient.getAggregateTrades(SYMBOL, id, null, null, 1);
            AggregateTrade prevAggTrade = aggTradesInit.get(0);

            int lastDay = Instant.ofEpochMilli(prevAggTrade.getTime()).atZone(ZoneOffset.UTC).getDayOfMonth();

            List<AggregateTrade> oneDayAggTradesBatch = new ArrayList<>();


            System.out.println(syncRequestClient.getExchangeInformation()); // FIXME

            while (true) {
                List<AggregateTrade> aggTrades = null;
                try {
                aggTrades = syncRequestClient.getAggregateTrades(SYMBOL, id, null, null, 1000);
                } catch (BinanceApiException bae) {
                    Thread.sleep(61_000);
                    continue;
                }

                for (AggregateTrade aggTrade : aggTrades) {
                    //log.info("Date/Time: " + formatDateTimeUTC(aggTrade.getTime()) + " >> " + aggTrade.toString());


                    int currDay = Instant.ofEpochMilli(aggTrade.getTime()).atZone(ZoneOffset.UTC).getDayOfMonth();

                    if (lastDay != currDay) {

                        log.info("writing csv: " + getFileName(prevAggTrade));
                        writeCSV(getFileName(prevAggTrade), oneDayAggTradesBatch);

                        oneDayAggTradesBatch.clear();

                        lastDay = currDay;
                    }

                    oneDayAggTradesBatch.add(aggTrade);
                    prevAggTrade = aggTrade;
                }
                Thread.sleep(1);

                long lastId = aggTrades.get(aggTrades.size()-1).getId();

                if (lastId - id < 999) {
                    log.info("Stopping!!!! lastId: " + lastId + ", id: " + id);
                    log.info("writing csv: " + getFileName(prevAggTrade));
                    writeCSV(getFileName(prevAggTrade), oneDayAggTradesBatch);
                    break;
                }

                id = lastId + 1;

//                if (lastId > 93_000_000L) {  // 94_652_727  (~9:40:00 UTC)
//                    break;
//                }
            }
        } catch (InterruptedException e) {
            log.error(e.toString());
        }

//        ZonedDateTime dt = parseDate8("2018-04-01T17:26:54.022Z");
//
//        List<AggregateTrade> aggTrades = syncRequestClient.getAggregateTrades("BTCUSDT", null, dt.toInstant().toEpochMilli(), null, 500);
//
//        for (AggregateTrade aggTrade : aggTrades) {
//            log.info("Date/Time: " + formatDateTimeUTC(aggTrade.getTime()) + " >> " + aggTrade.toString());
//        }



//        List<AggregateTrade> aggTrades = syncRequestClient.getAggregateTrades("BTCUSDT", null, null, null, 500);
//
//        for (AggregateTrade aggTrade : aggTrades) {
//            log.info("Date/Time: " + formatDateTimeUTC(aggTrade.getTime()) + " >> " + aggTrade.toString());
//        }

//        ZonedDateTime dt = parseDate8("2020-05-21T17:26:54.022Z");
//
//        dt.minusMonths(1);
//
//
//
//        log.info(dt.toString());
//        log.info(dt.toInstant().toEpochMilli() + "");


    }



}
