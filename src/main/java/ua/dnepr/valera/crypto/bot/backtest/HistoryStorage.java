package ua.dnepr.valera.crypto.bot.backtest;


import com.binance.client.model.market.AggregateTradeMini;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static ua.dnepr.valera.crypto.bot.Utils.parseDate;

public class HistoryStorage {

    private static final String COMMA_DELIMITER = ",";

    private String symbol;
    private boolean reduced;

    private List<AggregateTradeMini> history = new LinkedList<>();
    private Iterator<AggregateTradeMini> iterator;
    private long historySize = 0;
    private long currHistoryItem = 0;
    private long lastPercent;

    public HistoryStorage(String symbol, boolean reduced) {
        this.symbol = symbol;
        this.reduced = reduced;
    }

    public void init(String from, String to) {
        // load data from CSV
        // c:\Projects\Crypto\production_history\BTCUSDT\
        // history-0095973302_2020-05-24
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        long loadStart = System.currentTimeMillis();
        try (Stream<Path> paths = Files.walk(Paths.get("c:/Projects/Crypto/production_history/" + symbol + (reduced ? "-reduced" : "")))) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        String fileDateStr = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.lastIndexOf("_") + 1 + 10);
//                        System.out.println(fileName);
//                        System.out.println(fileDateStr);
                        LocalDate fileDate = parseDate(fileDateStr);
                        if (fileDate.isAfter(fromDate) && fileDate.isBefore(toDate) || fileDate.isEqual(fromDate) || fileDate.isEqual(toDate)) {
                            return true;
                        } else {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        history.addAll(loadDataFromCSV(path));
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        long loadEnd = System.currentTimeMillis();
        System.out.println("History Load finished in " + ((loadEnd - loadStart) / 1000) +  " sec.");

        System.out.println("HistoryStorage: Loaded " + history.size() + " records of history.");
        iterator = history.iterator();
        historySize = history.size();
    }

    private List<AggregateTradeMini> loadDataFromCSV(Path path) {
        System.out.println("Loading data from: " + path);
        String line;
        boolean firstLine = true;
        List<AggregateTradeMini> aggregateTrades = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toString()))) {

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] splittedLine = line.split(COMMA_DELIMITER);
                //id,price,qty,firstId,lastId,time,isBuyerMaker
                //73698589,8784.05,0.032,100648193,100648194,1588204805449,2020-04-30T00:00:05.449Z,true
//                AggregateTrade aggregateTrade = new AggregateTrade(Long.valueOf(splittedLine[0]), new BigDecimal(splittedLine[1]), new BigDecimal(splittedLine[2]),
//                        Long.valueOf(splittedLine[3]), Long.valueOf(splittedLine[4]), Long.valueOf(splittedLine[5]), Boolean.valueOf(splittedLine[7]));

//                AggregateTrade aggregateTrade = new AggregateTrade(Long.valueOf(splittedLine[0]), new BigDecimal(splittedLine[1]), new BigDecimal(splittedLine[2]),
//                        null, null, Long.valueOf(splittedLine[5]), Boolean.valueOf(splittedLine[7]));

                AggregateTradeMini aggregateTrade = new AggregateTradeMini(new BigDecimal(splittedLine[1]), reduced ? Long.valueOf(splittedLine[3]) : Long.valueOf(splittedLine[5]));
                aggregateTrades.add(aggregateTrade);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aggregateTrades;
    }

    public AggregateTradeMini getNext() {
        currHistoryItem++;
        long currPercent = (long)(currHistoryItem * 1.00 / historySize * 100);

        if (currPercent - lastPercent >= 1) {
            lastPercent = currPercent;
            //System.out.println(Utils.formatDateTimeUTCForPrint(System.currentTimeMillis()) + String.format("  Current percent: %2d", lastPercent) + " %"); // FIXME uncomment for batch Bot mode
        }

        return iterator.next();
    }



    public boolean hasNext() {
        return iterator.hasNext();
    }

    public void rewind() {
        currHistoryItem = 0;
        lastPercent =0;
        iterator = history.iterator();
    }

}
