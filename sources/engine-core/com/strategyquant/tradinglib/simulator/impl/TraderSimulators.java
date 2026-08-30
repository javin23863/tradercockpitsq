package com.strategyquant.tradinglib.simulator.impl;

import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;

public class TraderSimulators {
   public static ITradingSimulator getMT4Simulator() {
      return new MetaTrader4Simulator();
   }

   public static ITradingSimulator getMT5Simulator(boolean var0, byte var1) {
      return var0 ? new MetaTrader5SimulatorHedging(var1) : new MetaTrader5SimulatorNetting(var1);
   }

   public static ITradingSimulator getSimulator(boolean var0, boolean var1, byte var2) {
      return var0 ? getMT5Simulator(var1, var2) : getMT4Simulator();
   }

   public static ITradingSimulator getSimulator(ChartSetup var0) throws Exception {
      AbstractTradingSimulator var1 = null;
      switch (var0.getBacktestEngine()) {
         case -1816889229:
         case 1316847364:
            var1 = new MetaTrader4Simulator();
            break;
         case -659455871:
            var1 = new MetaTrader4Simulator();
            break;
         case 56756755:
            var1 = new TradestationSimulator();
            break;
         case 395961824:
            var1 = new MetaTrader5SimulatorHedging((byte)4);
            break;
         case 938213070:
            var1 = new MultiChartsSimulator();
            break;
         case 1441180233:
            var1 = new MetaTrader5SimulatorNetting((byte)4);
            break;
         case 1949519549:
            var1 = new JForexSimulator();
            break;
         default:
            throw new Exception("Unknown backtest engine with code " + var0.getBacktestEngine());
      }

      var1.setTestPrecision(var0.getTestPrecision());
      return var1;
   }
}
