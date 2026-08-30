package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.engine.TradingSetup;

public class BacktestInitStrategy extends StrategyBase {
   @Override
   public void callOnInit(TradingSetup var1) throws Exception {
      this.tradingSetup = var1;
      this.initializeFromMarketData(var1.getMarketData());
      this.dismissBadStrategies = var1.getDismissBadStrategies();
      this.warningsBadStrategies = var1.getWarningsBadStrategies();
      this.Initialize();
   }

   @Override
   public void Initialize() throws Exception {
   }

   @Override
   public void OnBarUpdate() throws Exception {
   }

   @Override
   public double getATRValue(ChartData var1, int var2, int var3) throws TradingException {
      return 0.0;
   }
}
