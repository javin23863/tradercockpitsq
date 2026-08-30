package com.strategyquant.tradinglib.results.stats.type;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderTypes;
import java.io.Serializable;

public class StatsTypeDirection implements IStatsType, Serializable {
   private final byte[] keys = new byte[]{0, 1, -1};

   @Override
   public Order filterTrades(byte var1, Order var2, SettingsMap var3) throws Exception {
      if (var1 == 0) {
         return var2;
      } else if (var1 == 1) {
         return OrderTypes.isLongOrder(var2.Type) ? var2 : null;
      } else if (var1 == -1) {
         return OrderTypes.isShortOrder(var2.Type) ? var2 : null;
      } else {
         throw new Exception("Unknown key '" + var1 + "' in trade filter!");
      }
   }

   @Override
   public byte[] getKeys() {
      return this.keys;
   }
}
