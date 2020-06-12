package com.binance.client.model.market;

import com.binance.client.constant.BinanceApiConstants;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class AggregateTrade {

    public AggregateTrade() {

    }

    public AggregateTrade(Long id, BigDecimal price, BigDecimal qty, Long firstId, Long lastId, Long time, Boolean isBuyerMaker) {
        this.id = id;
        this.price = price;
        this.qty = qty;
        this.firstId = firstId;
        this.lastId = lastId;
        this.time = time;
        this.isBuyerMaker = isBuyerMaker;
    }

    private Long id;

    private BigDecimal price;

    private BigDecimal qty;

    private Long firstId;

    private Long lastId;

    private Long time;

    private Boolean isBuyerMaker;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Long getFirstId() {
        return firstId;
    }

    public void setFirstId(Long firstId) {
        this.firstId = firstId;
    }

    public Long getLastId() {
        return lastId;
    }

    public void setLastId(Long lastId) {
        this.lastId = lastId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Boolean isBuyerMaker() {
        return isBuyerMaker;
    }

    public void setBuyerMaker(Boolean isBuyerMaker) {
        this.isBuyerMaker = isBuyerMaker;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("id", id)
                .append("price", price).append("qty", qty).append("firstId", firstId).append("lastId", lastId)
                .append("time", time).append("isBuyerMaker", isBuyerMaker).toString();
    }
}
