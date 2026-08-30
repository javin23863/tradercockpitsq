package com.strategyquant.tradinglib.equitychartnew.addons;

import com.strategyquant.pluginlib.ISQPlugin;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import java.io.Serializable;
import java.util.Map;

public interface IEquityChartAddon extends ISQPlugin, Serializable {
   void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception;

   String getName();
}
