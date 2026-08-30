package com.strategyquant.tradinglib.results.stats.computer;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.results.SymbolsMap;

public interface IOrderComputer {
   void compute(OrdersList var1, SettingsMap var2, SymbolsMap var3) throws Exception;
}
