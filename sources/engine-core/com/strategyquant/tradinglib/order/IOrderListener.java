package com.strategyquant.tradinglib.order;

public interface IOrderListener {
   void onOrderAdd(long var1, String var3, String var4, double var5, double var7);

   void onOrderClosed(long var1);

   void onOrderChanged(long var1, String var3, double var4, double var6);
}
