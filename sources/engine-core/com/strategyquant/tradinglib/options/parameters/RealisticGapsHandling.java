package com.strategyquant.tradinglib.options.parameters;

import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class RealisticGapsHandling extends TradingOption {
   @Parameter(name = "Realistic Gaps Handling", defaultValue = "false", category = "Trading options")
   @Help(
      "Only for MT engines - backtester in MetaTrader4/5 handles gaps in a way that is different from real trading (except when using MT5 engine with tick precision).\r\nWhen there is a gap, it fills Stop/Limit order at its desired open price, instead of at price after the gap.\r\nIn reality the order is fillled at price after the gap.\r\nNote that by turning this on your backtest in SQ might differ from backtest in MT, but it will be more realistic"
   )
   @ForEngine("MT4,MT5")
   public boolean RealisticGapsHandling;

   public RealisticGapsHandling() {
      this.inbuild = true;
   }

   @Override
   public boolean OnBarUpdate(StrategyBase var1) {
      return true;
   }

   @Override
   public boolean isUsedInTrading() {
      return false;
   }

   @Override
   public TradingOption getClone() {
      RealisticGapsHandling var1 = new RealisticGapsHandling();
      var1.RealisticGapsHandling = this.RealisticGapsHandling;
      return var1;
   }
}
