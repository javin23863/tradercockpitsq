package com.strategyquant.tradinglib.results;

import com.strategyquant.tradinglib.backtest.IBacktester;
import com.strategyquant.tradinglib.servlet.IServletPlugin;

public interface IBacktestPlugin extends IServletPlugin {
   void setBacktester(IBacktester var1);

   void setResultsGroupProvider(IResultsGroupProvider var1);
}
