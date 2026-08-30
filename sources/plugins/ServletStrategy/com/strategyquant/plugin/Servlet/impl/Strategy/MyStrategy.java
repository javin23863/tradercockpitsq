package com.strategyquant.plugin.Servlet.impl.Strategy;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.engine.TradingSetup;

public class MyStrategy extends StrategyBase {
   @Parameter
   public int Period;
   private int count = 0;

   public void Initialize() throws Exception {
   }

   public void OnBarUpdate() throws Exception {
      this.count++;
      this.Log.info("BAR UPDATE " + this.count);
   }

   public void callOnInit(TradingSetup var1) throws Exception {
   }

   public double getATRValue(ChartData var1, int var2, int var3) throws TradingException {
      return 0.0;
   }
}
