package com.strategyquant.tradinglib.engine;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.backtest.BacktestTradingSetupRunnable;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.ReservedBars;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.simulator.Engines;
import java.util.ArrayList;
import java.util.List;

public class BacktestTradingSetup extends TradingSetup {
   private BacktestTradingSetupRunnable backtestRunnable;

   public BacktestTradingSetup(BacktestEngine var1, ChartSetup var2, SettingsMap var3, StrategyBase var4) {
      super(null, var1.getEngineId(), var2, var3, var4);
      this.recognizeUserDefinedStartingBar(var3);
      this.setTradingType(1);
   }

   private void recognizeUserDefinedStartingBar(SettingsMap var1) {
      this.indyStartingBar = 0;
      if (Engines.isTradestationEngine(this.getEngineId()) && var1 != null) {
         this.indyStartingBar = var1.getInt("StartingBar", 0);
         if (this.indyStartingBar < 0) {
            this.indyStartingBar = 0;
         }

         int var2 = var1.getInt("ReservedBars", 0);
         if (var2 == 0) {
            var2 = this.tryRecognizeReservedBarsFromTradingOptions(var1);
         }

         this.indyStartingBar += var2;
      }
   }

   private int tryRecognizeReservedBarsFromTradingOptions(SettingsMap var1) {
      ArrayList var2 = (ArrayList)var1.get("TradingOptions");
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            TradingOption var4 = (TradingOption)var2.get(var3);
            if (var4 instanceof ReservedBars) {
               ReservedBars var5 = (ReservedBars)var4;
               return var5.ReservedBars;
            }
         }
      }

      return 0;
   }

   public void initRunnable(IDataBuffer var1) {
      this.backtestRunnable = new BacktestTradingSetupRunnable(this, var1, this.usePreparedData);
   }

   public void processEvent(TickEvent var1, boolean var2) throws Exception {
      this.backtestRunnable.runCalled(var1, var2);
   }

   void setTradingEngine(TradingEngine var1) {
      this.tradingEngine = var1;
   }

   @Override
   public void setBarEventType(int var1) {
      super.setBarEventType(var1);
      this.backtestRunnable.setBarEventType(var1);
   }

   public BarUpdateInfo getBarUpdateInfo() {
      return this.backtestRunnable.getBarUpdateInfo();
   }

   public void initTradingOptions() throws Exception {
      if (this.settings.containsKey("TradingOptions") && this.settings.get("TradingOptions") instanceof TradingOptions) {
         TradingOptions var1 = (TradingOptions)this.settings.get("TradingOptions");
         setTradingOptionsSettings(this.settings, var1);
      }

      this.initInternalSettings();
   }

   public static void setTradingOptionsSettings(SettingsMap var0, TradingOptions var1) throws Exception {
      if (var1 != null && var0 != null) {
         for (TradingOption var3 : var1) {
            String var4 = var3.getClass().getSimpleName();
            List var5 = var3.getParametersList();

            for (String var7 : var5) {
               String var8;
               if (var5.size() <= 1 && var4.equals(var7)) {
                  var8 = var7;
               } else {
                  var8 = String.format("%s.%s", var4, var7);
               }

               var0.set(var8, var3.getParameterValue(var7));
            }
         }
      }
   }

   public String getChartsHash() {
      StringBuilder var1 = new StringBuilder(0);
      return var1.toString();
   }
}
