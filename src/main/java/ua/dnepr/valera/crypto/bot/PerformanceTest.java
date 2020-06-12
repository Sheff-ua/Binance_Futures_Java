package ua.dnepr.valera.crypto.bot;

import com.binance.client.model.market.AggregateTrade;
import com.binance.client.model.market.AggregateTradeMini;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTest {
     private static final long MEGABYTE = 1024L * 1024L;

     public static long bytesToMegabytes(long bytes) {
         return bytes / MEGABYTE;
     }

     public static void main(String[] args) {
         // I assume you will know how to create a object Person yourself...
         List<AggregateTrade> list = new ArrayList<>();
         List<AggregateTradeMini> listMini = new ArrayList<>();
         for (int i = 0; i <= 10000000; i++) {
             list.add(new AggregateTrade(null, new BigDecimal(i+""), null, null, null, Long.valueOf(i+4+""), null));
             //listMini.add(new AggregateTradeMini(new BigDecimal(i+""), Long.valueOf(i+4+"")));
         }
         // Get the Java runtime
         Runtime runtime = Runtime.getRuntime();
         // Run the garbage collector
         runtime.gc();
         // Calculate the used memory
         long memory = runtime.totalMemory() - runtime.freeMemory();
         System.out.println("Used memory is bytes: " + memory);
         System.out.println("Used memory is megabytes: " + bytesToMegabytes(memory));
     }
 }