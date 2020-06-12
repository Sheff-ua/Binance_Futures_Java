package com.binance.client.model.enums;

import com.binance.client.impl.utils.EnumLookup;

/**
 * buy-market, sell-market, buy-limit, buy-ioc, sell-ioc,
 * buy-limit-maker, sell-limit-maker, buy-stop-limit, sell-stop-limit.
 */
public enum OrderType {
    LIMIT("LIMIT"),
    MARKET("MARKET"),
    STOP("STOP"),
    STOP_MARKET("STOP_MARKET"),
    TAKE_RPOFIT("TAKE_RPOFIT"),
    TAKE_RPOFIT_MARKET("TAKE_RPOFIT_MARKET"),

    // absent
    STOP_LOSS("STOP_LOSS"),
    STOP_LOSS_LIMIT("STOP_LOSS_LIMIT"),
    TAKE_PROFIT_LIMIT("TAKE_PROFIT_LIMIT"),
    LIMIT_MAKER("LIMIT_MAKER"),
    LIQUIDATION("LIQUIDATION"),
    INVALID(null);

  private final String code;

  OrderType(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return code;
  }

  private static final EnumLookup<OrderType> lookup = new EnumLookup<>(OrderType.class);

  public static OrderType lookup(String name) {
    return lookup.lookup(name);
  }

}
