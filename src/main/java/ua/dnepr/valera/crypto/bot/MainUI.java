package ua.dnepr.valera.crypto.bot;

import com.binance.client.*;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.enums.*;
import com.binance.client.model.event.AggregateTradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainUI {
    private static final Logger log = LoggerFactory.getLogger(MainUI.class);

    private static String listenKey;
    private static SyncRequestClient syncRequestClient;




    public static void main(String[] args) {


        /****** UI *********/
        //Creating the Frame
        JFrame frame = new JFrame("Futures API");
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
                //syncRequestClient.keepUserDataStream(listenKey);
                //syncRequestClient.closeUserDataStream(listenKey);

                // place dual position side order.
                // Switch between dual or both position side, call: com.binance.client.examples.trade.ChangePositionSide
                log.info(syncRequestClient.postOrder("BTCUSDT", OrderSide.BUY, PositionSide.LONG, OrderType.LIMIT, TimeInForce.GTC,
                        "0.001", "5000", null, "keepAlive1", null, null, NewOrderRespType.RESULT).toString());
            }
        });

        /***************/

        SubscriptionClient client = SubscriptionClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY);


        SubscriptionErrorHandler errorHandler = new SubscriptionErrorHandler() {
            @Override
            public void onError(BinanceApiException exception) {
                log.error("AggregateTradeEventSubscriptionErrorHandler: " + exception);
            }
        };

        SubscriptionListener<AggregateTradeEvent> aggregateTradeEventListener = new SubscriptionListener<AggregateTradeEvent>() {
            int i = 0;

            @Override
            public void onReceive(AggregateTradeEvent event) {
                if (i++ % 20 == 0) {
                    log.info(event.toString());
                }
            }
        };

        //client.subscribeAggregateTradeEvent("btcusdt", aggregateTradeEventListener, errorHandler);


        // TODO reconnect streams every 24 hour
        /*********************************************/

        RequestOptions options = new RequestOptions();
        syncRequestClient = SyncRequestClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, options);

        // Start user data stream
        listenKey = syncRequestClient.startUserDataStream();
        log.info("listenKey: " + listenKey);

        // Keep user data stream
        syncRequestClient.keepUserDataStream(listenKey);

        // Close user data stream
        //syncRequestClient.closeUserDataStream(listenKey);
        SubscriptionOptions so = new SubscriptionOptions(true, 60_000, 10);
        SubscriptionClient client1 = SubscriptionClient.create(PrivateConfig.API_KEY, PrivateConfig.SECRET_KEY, so);

        SubscriptionErrorHandler errorHandler1 = new SubscriptionErrorHandler() {
            @Override
            public void onError(BinanceApiException exception) {
                log.error("UserDataEventSubscriptionErrorHandler: " + exception);
            }
        };

        client1.subscribeUserDataEvent(listenKey, ((event) -> {
            log.info(event.toString());
        }), errorHandler1);

        /*********************************************/


    }

}
