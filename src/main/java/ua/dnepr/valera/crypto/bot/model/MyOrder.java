package ua.dnepr.valera.crypto.bot.model;

import java.math.BigDecimal;
import java.util.Objects;

public class MyOrder {

    private String clientOrderId;

    private BigDecimal amount;

    private BigDecimal price;

    private boolean reduceOnly;

    private Side side;

    private Status status;

    private String symbol;

    private Type type;

    private Long updateTime;

    public MyOrder(String clientOrderId, BigDecimal amount, BigDecimal price, boolean reduceOnly, Side side, Status status, String symbol, Type type, Long updateTime) {
        this.clientOrderId = clientOrderId;
        this.amount = amount;
        this.price = price;
        this.reduceOnly = reduceOnly;
        this.side = side;
        this.status = status;
        this.symbol = symbol;
        this.type = type;
        this.updateTime = updateTime;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isReduceOnly() {
        return reduceOnly;
    }

    public Side getSide() {
        return side;
    }

    public Status getStatus() {
        return status;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public Type getType() {
        return type;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public enum Side {
        BUY,
        SELL
    }

    public enum Type {
        LIMIT,
        MARKET,
        STOP_MARKET
    }

    public enum Status {
        NEW,
        FILLED,
        CANCELLED
        /*PARTIALLY_FILLED,*/
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyOrder myOrder = (MyOrder) o;
        return clientOrderId.equals(myOrder.clientOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientOrderId);
    }

    @Override
    public String toString() {
        return "MyOrder{" +
                "clientOrderId='" + clientOrderId + '\'' +
                ", amount=" + amount +
                ", price=" + price +
                ", reduceOnly=" + reduceOnly +
                ", side=" + side +
                ", status=" + status +
                ", symbol='" + symbol + '\'' +
                ", type=" + type +
                ", updateTime=" + updateTime +
                '}';
    }
}
