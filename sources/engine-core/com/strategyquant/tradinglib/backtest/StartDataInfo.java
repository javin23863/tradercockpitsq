package com.strategyquant.tradinglib.backtest;

public class StartDataInfo {
   public long realDataStartTime;
   public int reservedBars;
   public long realDataEndTime;

   public void copyFrom(StartDataInfo var1) {
      this.realDataStartTime = var1.realDataStartTime;
      this.reservedBars = var1.reservedBars;
      this.realDataEndTime = var1.realDataEndTime;
   }
}
