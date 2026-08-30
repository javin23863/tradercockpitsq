package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TradingException;

public class TimeDataSeries extends TimeDataSeriesBase {
   public TimeDataSeries() {
   }

   public TimeDataSeries(int var1) {
      super(var1);
   }

   public TimeDataSeries(String var1, int var2) {
      super(var2, var1);
   }

   public TimeDataSeries(String var1, int var2, ChartDef var3) {
      super(var2, var1, var3);
   }

   public TimeDataSeries(int var1, int var2) {
      super(var1, var2);
   }

   public TimeDataSeries(String var1) {
      super(100);
      this.setName(var1);
   }

   public long get(int var1) throws TradingException {
      return this._getLong(var1);
   }

   public void set(int var1, long var2) throws TradingException {
      this._setLong(var1, var2);
   }

   public void addValues(int var1) throws TradingException {
      this.addLongValues(var1, 0L);
   }

   public void add(long var1) throws TradingException {
      this._addLong(var1);
   }
}
