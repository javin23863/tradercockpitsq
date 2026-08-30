package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades;

import java.util.ArrayList;

public class OverlappingResult {
   public String symbol1;
   public String symbol2;
   public int count = 0;
   public ArrayList<OverlappingTrade> data = new ArrayList<>();
   public boolean selected = false;

   public OverlappingResult(String var1, String var2) {
      this.symbol1 = var1;
      this.symbol2 = var2;
   }
}
