package com.strategyquant.tradinglib.connection;

import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;

public class ConnectionPluginManager {
   public static IConnectionPlugin getConnectionPlugin(String var0) throws Exception {
      for (IConnectionPlugin var3 : SQPluginManager.getPlugins(IConnectionPlugin.class)) {
         if (var3.getPluginName().equalsIgnoreCase(var0)) {
            return var3;
         }
      }

      throw new Exception("ConnectionPlugin named '" + var0 + "' was not found!");
   }
}
