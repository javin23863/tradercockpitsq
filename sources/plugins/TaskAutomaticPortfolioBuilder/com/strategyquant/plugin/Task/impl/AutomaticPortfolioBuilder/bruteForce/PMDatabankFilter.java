package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DefaultDatabankFilter;

public class PMDatabankFilter extends DefaultDatabankFilter {
   private byte sampleType;

   public PMDatabankFilter(int var1, boolean var2, byte var3) {
      super(var1, var2);
      this.sampleType = var3;
   }

   public double getRankValue(ResultsGroup var1) {
      try {
         return var1.getFitness(this.sampleType);
      } catch (Exception var3) {
         Log.error("Cannot calculate rank value of strategy '" + var1.getName() + "'", var3);
         return 0.0;
      }
   }
}
