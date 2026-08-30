package com.strategyquant.tradinglib.generator;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.engine.TradingSetup;
import org.jdom2.Element;

public class StrategyWithVariables extends StrategyBase {
   public StrategyWithVariables(Element var1) {
      this.variables = new Variables(var1);
   }

   @Override
   public void callOnInit(TradingSetup var1) throws Exception {
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
