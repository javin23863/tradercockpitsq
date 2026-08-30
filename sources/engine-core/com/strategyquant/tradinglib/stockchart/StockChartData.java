package com.strategyquant.tradinglib.stockchart;

import java.util.List;

public class StockChartData {
   private String title;
   private List<Double> open;
   private List<Double> close;
   private List<Double> high;
   private List<Double> low;
   private long[] xVals;
   private Indicator[] indicators;
   private Trade[] trades;
   private int precision = 5;

   public Trade[] getTrades() {
      return this.trades;
   }

   public void setTrades(Trade[] var1) {
      this.trades = var1;
   }

   public String getTitle() {
      return this.title;
   }

   public void setTitle(String var1) {
      this.title = var1;
   }

   public List<Double> getOpen() {
      return this.open;
   }

   public void setOpen(List<Double> var1) {
      this.open = var1;
   }

   public List<Double> getClose() {
      return this.close;
   }

   public void setClose(List<Double> var1) {
      this.close = var1;
   }

   public List<Double> getHigh() {
      return this.high;
   }

   public void setHigh(List<Double> var1) {
      this.high = var1;
   }

   public List<Double> getLow() {
      return this.low;
   }

   public void setLow(List<Double> var1) {
      this.low = var1;
   }

   public Indicator[] getIndicators() {
      return this.indicators;
   }

   public void setIndicators(Indicator[] var1) {
      this.indicators = var1;
   }

   public long[] getxVals() {
      return this.xVals;
   }

   public void setxVals(long[] var1) {
      this.xVals = var1;
   }

   public int getPrecision() {
      return this.precision;
   }

   public void setPrecision(int var1) {
      this.precision = var1;
   }
}
