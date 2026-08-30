package com.strategyquant.tradinglib.results.stats.type;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;

public interface IStatsType {
   Order filterTrades(byte var1, Order var2, SettingsMap var3) throws Exception;

   byte[] getKeys();
}
