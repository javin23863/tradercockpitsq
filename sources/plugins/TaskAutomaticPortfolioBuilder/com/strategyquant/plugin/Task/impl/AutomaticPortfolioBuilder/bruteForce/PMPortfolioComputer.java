package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
import java.util.ArrayList;

public class PMPortfolioComputer {
   public ResultsGroup createPortfolio(ArrayList<ResultsGroup> var1, String var2, PortfolioMasterSettings var3) throws Exception {
      MoneyManagementMethod var4 = null;
      if (var3.mmMethodOptional) {
         var4 = var3.mmMethod;
      }

      return StrategyMerger.createSimulatedResult(null, var1, var2, var3.initialCapital, var4);
   }
}
