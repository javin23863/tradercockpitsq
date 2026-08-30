package com.strategyquant.tradinglib.plugindef.connection;

import com.strategyquant.lib.ValuesMap;
import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;

public interface IConnectionPlugin extends ISQPlugin {
   String getPluginName();

   boolean isDataFeed();

   boolean isExecutionEngine();

   Connection createConnection(ConnectionManager var1, String var2, ValuesMap var3) throws Exception;
}
