package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.correlation;

public class CorrelationResult {
   public String symbol1;
   public String symbol2;
   public double value;

   public CorrelationResult(String var1, String var2) {
      this.symbol1 = var1;
      this.symbol2 = var2;
   }
}
