package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;

public class MedianDataSeries extends ComputedDataSeries {
   public MedianDataSeries(DataSeries var1, DataSeries var2, DataSeries var3, DataSeries var4, DataSeries var5) {
      super(var1, var2, var3, var4, var5);
   }

   @Override
   public double computeValue(int var1) throws TradingException {
      return this.High != null && this.Low != null ? (this.High.get(var1) + this.Low.get(var1)) / 2.0 : -1.0;
   }
}
