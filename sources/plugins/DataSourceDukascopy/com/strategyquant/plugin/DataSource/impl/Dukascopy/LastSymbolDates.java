package com.strategyquant.plugin.DataSource.impl.Dukascopy;

import java.io.Serializable;

public class LastSymbolDates implements Serializable {
   String symbol;
   long dateFrom;
   long dateTo;

   public LastSymbolDates(String var1, long var2, long var4) {
      this.symbol = var1;
      this.dateFrom = var2;
      this.dateTo = var4;
   }
}
