package com.strategyquant.tradinglib.results.stats.type;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;
import java.io.Serializable;

public class StatsTypePL implements IStatsType, Serializable {
   private final byte[] keys = new byte[]{10, 20, 30};

   @Override
   public Order filterTrades(byte var1, Order var2, SettingsMap var3) throws Exception {
      return var2;
   }

   @Override
   public byte[] getKeys() {
      return this.keys;
   }
}
