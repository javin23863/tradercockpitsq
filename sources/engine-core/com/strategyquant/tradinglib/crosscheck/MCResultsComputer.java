package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.robustnesstests.RobustnessOrdersValues;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;

public class MCResultsComputer {
   protected static final StatsTypeCombination combination = new StatsTypeCombination((byte)0, (byte)10, (byte)127);

   public static RobustnessResults computeResults(OrdersList var0, SettingsMap var1, SymbolsMap var2, Result var3) throws Exception {
      RobustnessResults var4 = new RobustnessResults();
      StatsComputer var5 = new StatsComputer();
      var5.initializeWithOrders(var0);
      if (var3 != null) {
         SQStats var6 = var3.stats((byte)0, (byte)10, (byte)127);
         double var7 = var6.getDouble("TotalTradingDays");
         double var9 = var6.getDouble("TotalTradingMonths");
         double var11 = var6.getDouble("TotalTradingYears");
         var1.set("TotalTradingDays", var7);
         var1.set("TotalTradingMonths", var9);
         var1.set("TotalTradingYears", var11);
      }

      var4.setOrdersValues(computeOrdersValues(var5, var1, var2));
      SQStats var13 = var5.computeStats(null, combination, var1);
      var4.setOriginalStats(var13);
      return var4;
   }

   public static RobustnessOrdersValues computeOrdersValues(StatsComputer var0, SettingsMap var1, SymbolsMap var2) throws Exception {
      var0.filterBy("Portfolio", (byte)0, (byte)127);
      var0.sortAndComputeOrderValues(var1, var2);
      OrdersList var3 = var0.getFilteredOrders();
      RobustnessOrdersValues var4 = new RobustnessOrdersValues(var3.size());

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Order var6 = var3.get(var5);
         var4.set(var5, var6.PL);
      }

      return var4;
   }
}
