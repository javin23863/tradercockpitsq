package com.strategyquant.tradinglib.results;

public abstract class AbstractResultsPlugin implements IResultPlugin {
   protected IResultsGroupProvider rgProvider;

   @Override
   public void setResultsGroupProvider(IResultsGroupProvider var1) {
      this.rgProvider = var1;
   }
}
