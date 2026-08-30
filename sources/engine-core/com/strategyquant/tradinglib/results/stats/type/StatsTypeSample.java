package com.strategyquant.tradinglib.results.stats.type;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;
import java.io.Serializable;

public class StatsTypeSample implements IStatsType, Serializable {
   private final byte[] keys = new byte[]{127};

   @Override
   public Order filterTrades(byte var1, Order var2, SettingsMap var3) throws Exception {
      return var2;
   }

   @Override
   public byte[] getKeys() {
      return this.keys;
   }
}
