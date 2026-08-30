package com.strategyquant.tradinglib.strategy;

import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Trader;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public class TraderUtils {
   public static OrdersList getHistoryOrders(Trader var0) {
      return var0.getHistoryOrders();
   }

   public static Long2FloatRBTreeMap getWorstDailyEquity(Trader var0) {
      return var0.getWorstDailyEquity();
   }
}
