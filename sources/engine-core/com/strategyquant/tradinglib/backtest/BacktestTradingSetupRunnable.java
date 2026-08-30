package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.tradinglib.engine.BacktestTradingSetup;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.setup.TradingSetupRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestTradingSetupRunnable extends TradingSetupRunnable {
   public static final Logger Log = LoggerFactory.getLogger("BacktestTradingSetupRunnable");

   public BacktestTradingSetupRunnable(BacktestTradingSetup var1, IDataBuffer var2, boolean var3) {
      super(var1, var3);
   }

   @Override
   public void run() {
      boolean var1 = true;
   }

   public void runCalled(TickEvent var1, boolean var2) throws Exception {
      this.runOneCycle(var1, var2);
   }

   public BarUpdateInfo getBarUpdateInfo() {
      return this.updateInfo;
   }
}
