package com.strategyquant.tradinglib.connection;

import com.strategyquant.datalib.ChartDef;

public interface IDataFeed {
   void registerSymbol(String var1) throws Exception;

   boolean checkSymbolIsSupported(String var1);

   HistoryData getHistoryData(ChartDef var1, int var2, long var3, long var5);
}
