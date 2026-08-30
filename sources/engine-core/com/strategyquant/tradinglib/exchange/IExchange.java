package com.strategyquant.tradinglib.exchange;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import java.io.Serializable;
import org.json.JSONArray;

public interface IExchange extends ISQPlugin, Serializable {
   String getName();

   JSONArray getSymbols() throws Exception;

   void cancel();

   void pause();

   void restart();

   void performDownload(String var1, DataManagerProgressListener var2, DataInfo var3, ImportInfo var4) throws Exception;

   boolean isCanceled();

   IExchange clone();

   String convertTimeframe(String var1) throws Exception;

   JSONArray availableTimeframes();

   boolean checkSymbolExists(String var1);

   SymbolInfo getSymbolInfo(String var1);
}
