package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;

public class JForexSimulator extends MetaTrader4Simulator {
   @Override
   public void initSettings(SettingsMap var1) throws TradingException {
      super.initSettings(var1);
   }

   @Override
   public ITradingSimulator clone() {
      JForexSimulator var1 = new JForexSimulator();
      var1.setTestPrecision(this.getTestPrecision());
      return var1;
   }

   @Override
   protected boolean checkPriceLevelCorrectness(int var1, double var2, double var4) {
      if (var1 == 5) {
         if (var2 >= var4) {
            return false;
         }
      } else if (var1 == 6) {
         if (var2 <= var4) {
            return false;
         }
      } else if (var1 == 3) {
         if (var2 <= var4) {
            return false;
         }
      } else if (var1 == 4 && var2 >= var4) {
         return false;
      }

      return true;
   }

   @Override
   public boolean fillAtRealAskBidPrice() {
      return true;
   }

   @Override
   protected double getSLPTFilledPrice(double var1, double var3) {
      return var1;
   }

   @Override
   public int getEngineId() {
      return -659455871;
   }
}
