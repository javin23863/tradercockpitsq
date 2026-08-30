package com.strategyquant.tradinglib.backtest;

import com.strategyquant.tradinglib.ChartSetup;
import java.io.Serializable;
import java.util.Map;

public interface IBacktester {
   void run(String var1, ChartSetup var2, Map<String, Serializable> var3) throws Exception;

   void stop(String var1) throws Exception;

   void setServerInfo(String var1, int var2);
}
