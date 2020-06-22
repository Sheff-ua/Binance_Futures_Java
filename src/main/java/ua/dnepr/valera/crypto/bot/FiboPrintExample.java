package ua.dnepr.valera.crypto.bot;

import ua.dnepr.valera.crypto.bot.model.MyOrder;
import ua.dnepr.valera.crypto.bot.model.MyPosition;

import java.math.BigDecimal;

public class FiboPrintExample {

    public static void main(String[] args) {
        MyPosition longPosition = new MyPosition(MyPosition.Side.LONG, "BTCUSDT");

        // 1
        MyOrder clientOrderFilled = new MyOrder(1L, new BigDecimal("0.01"), new BigDecimal("9999"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.MARKET, 10L);
        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.println("Initial market: " + longPosition);


        // 2
        clientOrderFilled = new MyOrder(2L, new BigDecimal("0.02"), new BigDecimal("9764"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 20L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 1 at 9764:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9764"))));


        // 3
        clientOrderFilled = new MyOrder(3L, new BigDecimal("0.03"), new BigDecimal("9618"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 30L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 2 at 9618:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9618"))));


        // 4
        clientOrderFilled = new MyOrder(4L, new BigDecimal("0.04"), new BigDecimal("9500"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 40L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 3 at 9500:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9500"))));


        // 5
        clientOrderFilled = new MyOrder(5L, new BigDecimal("0.05"), new BigDecimal("9382"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 50L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 4 at 9382:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9382"))));


        // 6
        clientOrderFilled = new MyOrder(6L, new BigDecimal("0.06"), new BigDecimal("9214"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 60L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 5 at 9214:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9214"))));

        // 7
        clientOrderFilled = new MyOrder(7L, new BigDecimal("0.07"), new BigDecimal("9000"),
                false, MyOrder.Side.BUY, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 70L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("Avg 6 at 9000:  " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9000"))));


//        System.out.println("      Unrealized PnL at 8900:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("8900"))));
//        // 8 stop
//        clientOrderFilled = new MyOrder(8L, longPosition.getAmount(), new BigDecimal("8900"),
//                false, MyOrder.Side.SELL, MyOrder.Status.FILLED,
//                "BTCUSDT", MyOrder.Type.STOP_MARKET, 80L);
//
//        longPosition.addOrderToHistory(clientOrderFilled);
//        System.out.print("STOP at 8900:   " + longPosition);
//        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("8900"))));
//        System.out.println("      History:  \n" + longPosition.printEntryPriceAmountHistory());


        System.out.println("      Unrealized PnL at 9214:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9214"))));
        //System.out.println("      Unrealized PnL at 9400:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9400"))));
        // 8 profit at 9214
        clientOrderFilled = new MyOrder(8L, longPosition.getAmount(), new BigDecimal("9214"),
                false, MyOrder.Side.SELL, MyOrder.Status.FILLED,
                "BTCUSDT", MyOrder.Type.LIMIT, 80L);

        longPosition.addOrderToHistory(clientOrderFilled);
        System.out.print("PROFIT at 9214: " + longPosition);
        System.out.println("      Unrealized PnL at this Price:  " + Utils.formatPrice(longPosition.calcUnRealizedPNL(new BigDecimal("9214"))));
        System.out.println("      History:  \n" + longPosition.printEntryPriceAmountHistory());

    }

}
