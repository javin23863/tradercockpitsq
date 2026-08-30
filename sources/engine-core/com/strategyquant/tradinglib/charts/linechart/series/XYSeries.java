package com.strategyquant.tradinglib.charts.linechart.series;

public class XYSeries extends AbstractSeries {
   public XYSeries(String var1) {
      super(var1);
   }

   public XYSeries(String var1, int var2) {
      super(var1);
      this.capacity = var2;
   }

   public synchronized void addValue(long var1, double var3) {
      XYValue var5 = new XYValue(var1, var3, var1 + "");
      this.values.add(var5);
   }

   public synchronized void addValue(long var1, double var3, String var5) {
      XYValue var6 = new XYValue(var1, var3, var5);
      this.values.add(var6);
   }
}
