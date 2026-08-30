package com.strategyquant.tradinglib.dukascopy;

import com.strategyquant.datalib.InstrumentInfo;

public class ImportInfo {
   public String symbol;
   public String origSymbol;
   public long dateFrom;
   public long dateTo;
   public long oldDateFrom;
   public long oldDateTo;
   public InstrumentInfo instrumentInfo;
   public double priceConstant;
   public boolean overwrite;
   public boolean useCDN;
   public boolean downloadM1Data;
   public boolean ignoreWeekend;
   public int rows;
   public long fromTime;
   public long toTime;
   public String downloadType;
}
