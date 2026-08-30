package com.strategyquant.tradinglib.engine.stockpicker.backtester;

import java.io.Serializable;

public class PortfolioBacktestJobResult implements Serializable {
   public String symbol;
   public int totalTicks;

   public PortfolioBacktestJobResult(String var1, int var2) {
      this.symbol = var1;
      this.totalTicks = var2;
   }
}
