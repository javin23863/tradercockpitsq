package com.strategyquant.tradinglib.charts.linechart.series;

public class XYValue {
   public long x;
   public double y;
   public String xLabel;

   public XYValue(long var1, double var3) {
      this.x = var1;
      this.y = var3;
   }

   public XYValue(long var1, double var3, String var5) {
      this.x = var1;
      this.y = var3;
      this.xLabel = var5;
   }
}
