package com.strategyquant.tradinglib.equitychart;

public class DomainAxisType {
   public static final String TRADE = "trade";
   public static final String TIME = "time";

   public static String[] types() {
      return new String[]{"trade", "time"};
   }
}
