package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.StrategyBase;

public class StrategyParamData {
   private StrategyBase strategy;
   private String strParams;

   public StrategyParamData(StrategyBase var1, String var2) {
      this.strategy = var1;
      this.strParams = var2;
   }

   public StrategyBase getStrategy() {
      return this.strategy;
   }

   public String getParams() {
      return this.strParams;
   }
}
