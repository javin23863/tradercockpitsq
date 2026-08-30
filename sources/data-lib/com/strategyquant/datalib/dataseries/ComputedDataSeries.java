package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;

public abstract class ComputedDataSeries extends DataSeries {
   public DataSeries Open;
   public DataSeries High;
   public DataSeries Low;
   public DataSeries Close;
   public DataSeries Volume;

   public ComputedDataSeries(DataSeries var1, DataSeries var2, DataSeries var3, DataSeries var4, DataSeries var5) {
      super(0, 0);
      this.Open = var1;
      this.High = var2;
      this.Low = var3;
      this.Close = var4;
      this.Volume = var5;
   }

   @Override
   public double get(int var1) throws TradingException {
      return this.computeValue(var1);
   }

   public abstract double computeValue(int var1) throws TradingException;

   @Override
   public int size() {
      return this.Open.size();
   }

   @Override
   public void destroy() {
   }
}
