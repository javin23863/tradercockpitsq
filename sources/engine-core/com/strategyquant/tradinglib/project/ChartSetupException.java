package com.strategyquant.tradinglib.project;

public class ChartSetupException extends Exception {
   private int chartIndex;

   public ChartSetupException(int var1, Exception var2) {
      super(var2);
      this.chartIndex = var1;
   }

   public int getChartIndex() {
      return this.chartIndex;
   }
}
