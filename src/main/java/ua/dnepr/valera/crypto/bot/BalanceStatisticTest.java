package ua.dnepr.valera.crypto.bot;

import java.math.BigDecimal;

public class BalanceStatisticTest {
    public static void main(String[] args) {
        BigDecimal initialBalance = new BigDecimal("1000.00");


        BigDecimal maxDrawDown = BigDecimal.ZERO;
        BigDecimal maxDrawDownAgainstInitial = BigDecimal.ZERO;
        BigDecimal maxProfit = BigDecimal.ZERO;
        BigDecimal maxProfitAgainstInitial = BigDecimal.ZERO;

        BigDecimal maxBalance = new BigDecimal("1600.78");
        BigDecimal minBalance = new BigDecimal("1000.00");
        BigDecimal balance = new BigDecimal("1400");

        BigDecimal maxDrawDownAgainstInitialCandidate = Utils.calcXOfYInPercents(initialBalance.subtract(minBalance), initialBalance);
        if (maxDrawDownAgainstInitialCandidate.compareTo(maxDrawDownAgainstInitial) > 0) {
            maxDrawDownAgainstInitial = maxDrawDownAgainstInitialCandidate;
            System.out.println("maxDrawDownAgainstInitial: " + maxDrawDownAgainstInitial);
        }

        BigDecimal maxDrawDownCandidate = Utils.calcXOfYInPercents(maxBalance.subtract(balance), maxBalance);
        if (maxDrawDownCandidate.compareTo(maxDrawDown) > 0) {
            maxDrawDown = maxDrawDownCandidate;
            System.out.println("maxDrawDown: " + maxDrawDown);
        }

        BigDecimal maxProfitAgainstInitialCandidate = Utils.calcXOfYInPercents(maxBalance.subtract(initialBalance), initialBalance);
        if (maxProfitAgainstInitialCandidate.compareTo(maxProfitAgainstInitial) > 0) {
            maxProfitAgainstInitial = maxProfitAgainstInitialCandidate;
            System.out.println("maxProfitAgainstInitial: " + maxProfitAgainstInitial);
        }

        BigDecimal maxProfitCandidate = Utils.calcXOfYInPercents(balance.subtract(minBalance), minBalance);
        if (maxProfitCandidate.compareTo(maxProfit) > 0) {
            maxProfit = maxProfitCandidate;
            System.out.println("maxProfit: " + maxProfit);
        }
    }
}
