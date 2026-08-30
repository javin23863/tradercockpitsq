package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades;

public class OverlappingTrade {
   public long from1;
   public long to1;
   public long from2;
   public long to2;
   public String time;

   public OverlappingTrade(long var1, long var3, long var5, long var7, String var9) {
      this.from1 = var1;
      this.to1 = var3;
      this.from2 = var5;
      this.to2 = var7;
      this.time = var9;
   }
}
