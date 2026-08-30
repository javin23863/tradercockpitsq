package com.strategyquant.tradinglib.connection;

public interface IConnectionListener {
   void onConnectionAdd(String var1, String var2, String var3, int var4, double var5, double var7, String var9);

   void onConnectionPLChange(String var1, double var2);

   void onConnectionAccountBalanceChange(String var1, double var2);

   void onConnectionOpenOrdersChange(String var1, int var2);

   void onConnectionStateChange(String var1, String var2);

   void onConnectionError(String var1, Exception var2);

   void onConnectionRemove(String var1);
}
