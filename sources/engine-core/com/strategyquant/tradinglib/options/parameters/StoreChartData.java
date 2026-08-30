package com.strategyquant.tradinglib.options.parameters;

import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class StoreChartData extends TradingOption {
   @Parameter(name = "Store Chart Data", defaultValue = "false", category = "Build options")
   @Help("Store chart data in the backtest result?")
   public boolean StoreChartData;

   public StoreChartData() {
      this.inbuild = true;
   }

   @Override
   public boolean OnBarUpdate(StrategyBase var1) {
      return true;
   }

   public TradingOption clone() {
      StoreChartData var1 = new StoreChartData();
      var1.StoreChartData = this.StoreChartData;
      return var1;
   }

   @Override
   public boolean isUsedInTrading() {
      return false;
   }
}
