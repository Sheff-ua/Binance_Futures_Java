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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static ua.dnepr.valera.crypto.bot.Utils.parseDate;

public class HistoryStorage {

    private static final String COMMA_DELIMITER = ",";

    private String symbol;
    private boolean reduced;

    private List<AggregateTradeMini> history = new ArrayList<>();
    private int historySize = 0;

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

                AggregateTradeMini aggregateTrade = new AggregateTradeMini(new BigDecimal(splittedLine[1]), reduced ? Long.valueOf(splittedLine[3]) : Long.valueOf(splittedLine[5]));
                aggregateTrades.add(aggregateTrade);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aggregateTrades;
    }

    public int getHistorySize() {
        return historySize;
    }

    public AggregateTradeMini getStep(int step) {
        return history.get(step);
    }

    public int calcPercent(int step) {
        return  (int)(step * 1.00 / historySize * 100);
    }

}
