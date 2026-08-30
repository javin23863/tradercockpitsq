package com.strategyquant.tradinglib.strategy;

import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;

public class SimpleStrategyTypes extends JSONAble {
   public static final String DEFAULT_FOREX = "DefaultForex";
   public static final String DEFAULT_FUTURES = "DefaultFutures";
   public static final String DEFAULT_STOCKPICKER = "DefaultStockpicker";
   public static final String MARKET = "Market";
   public static final String TREND_FOLLOWING = "TrendFollowing";
   public static final String MEAN_REVERSAL = "Mean-Reversal";
   public static final String FUZZY = "Fuzzy";
   public static final String DAILY = "Daily";
   public static final String CUSTOM = "Custom";
}
