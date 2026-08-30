package com.strategyquant.tradinglib.results;

import com.strategyquant.tradinglib.backtest.IBacktester;

public abstract class AbstractBacktestPlugin implements IBacktestPlugin {
   protected IBacktester backtester;
   protected IResultsGroupProvider rgProvider;

   @Override
   public void setBacktester(IBacktester var1) {
      this.backtester = var1;
   }

   @Override
   public void setResultsGroupProvider(IResultsGroupProvider var1) {
      this.rgProvider = var1;
   }
}
