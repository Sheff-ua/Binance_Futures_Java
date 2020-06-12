package com.binance.client;

import com.binance.client.exception.BinanceApiException;
import java.net.URI;

/**
 * The configuration for the subscription APIs
 */
public class SubscriptionOptions {

    private boolean isAutoReconnect = true;
    private int receiveLimitMs = 60_000;
    private int connectionDelayOnFailure = 15;


    public SubscriptionOptions(boolean isAutoReconnect, int receiveLimitMs, int connectionDelayOnFailure) {
        this.isAutoReconnect = isAutoReconnect;
        this.receiveLimitMs = receiveLimitMs;
        this.connectionDelayOnFailure = connectionDelayOnFailure;
    }

    public SubscriptionOptions(SubscriptionOptions options) {
        this.isAutoReconnect = options.isAutoReconnect;
        this.receiveLimitMs = options.receiveLimitMs;
        this.connectionDelayOnFailure = options.connectionDelayOnFailure;
    }

    public SubscriptionOptions() {
    }

    /**
     * Set the receive limit in millisecond. If no message is received within this
     * limit time, the connection will be disconnected.
     *
     * @param receiveLimitMs The receive limit in millisecond.
     */
    public void setReceiveLimitMs(int receiveLimitMs) {
        this.receiveLimitMs = receiveLimitMs;
    }

    /**
     * If auto reconnect is enabled, specify the delay time before reconnect.
     *
     * @param connectionDelayOnFailure The delay time in second.
     */
    public void setConnectionDelayOnFailure(int connectionDelayOnFailure) {
        this.connectionDelayOnFailure = connectionDelayOnFailure;
    }

    /**
     * When the connection lost is happening on the subscription line, specify
     * whether the client reconnect to server automatically.
     * <p>
     * The connection lost means:
     * <ul>
     * <li>Caused by network problem</li>
     * <li>The connection close triggered by server (happened every 24 hours)</li>
     * <li>No any message can be received from server within a specified time, see
     * {@link #setReceiveLimitMs(int)} (int)}</li>
     * </ul>
     *
     * @param isAutoReconnect The boolean flag, true for enable, false for disable
     * @return Return self for chaining
     */
    public SubscriptionOptions setAutoReconnect(boolean isAutoReconnect) {
        this.isAutoReconnect = isAutoReconnect;
        return this;
    }

    public boolean isAutoReconnect() {
        return isAutoReconnect;
    }

    public int getReceiveLimitMs() {
        return receiveLimitMs;
    }

    public int getConnectionDelayOnFailure() {
        return connectionDelayOnFailure;
    }

}
