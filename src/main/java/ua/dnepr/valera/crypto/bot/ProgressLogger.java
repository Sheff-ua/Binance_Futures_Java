package ua.dnepr.valera.crypto.bot;

import java.util.SortedMap;
import java.util.TreeMap;

public class ProgressLogger {

    private SortedMap<Integer, Integer> exchangeProgressMap = new TreeMap<>();
    private long startTime;

    public ProgressLogger(long startTime) {
        this.startTime = startTime;
    }

    public void onProgress(int exchangeId, int newPercent) {
        long currTime = System.currentTimeMillis();
        exchangeProgressMap.put(exchangeId, newPercent);

        String output = "";

        for (Integer exchId : exchangeProgressMap.keySet()) {
            output = output + "Exchange " + (exchId+1) + " Progress " + exchangeProgressMap.get(exchId) + "%   ";
        }
        System.out.print("\r" + output + " Time passed: " + ((currTime - startTime) / 1000) +  " sec.");
    }

}
