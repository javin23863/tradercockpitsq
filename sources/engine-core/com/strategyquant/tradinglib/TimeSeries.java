package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.charts.linechart.series.AbstractSeries;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;

public class TimeSeries extends AbstractSeries {
   public TimeSeries(String var1) {
      super(var1);
   }

   public TimeSeries(String var1, int var2) {
      super(var1);
      this.capacity = var2;
   }

   public void addValue(long var1, double var3) {
      XYValue var5 = new XYValue(var1, var3);
      this.values.add(var5);
   }
}
