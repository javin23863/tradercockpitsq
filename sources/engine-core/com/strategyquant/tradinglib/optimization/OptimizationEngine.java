package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationEngine extends OptimizationEngineWF {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationEngine");

   public OptimizationEngine(String var1, StopPauseEngine var2, GridJob var3, String var4, ILastEventListener var5) {
      this(var1, var2, var3, var4, var5, null);
   }

   public OptimizationEngine(String var1, StopPauseEngine var2, GridJob var3, String var4, ILastEventListener var5, String var6) {
      super(var1, var2, var3, var4, var5, var6);
   }

   public void optimize() throws Exception {
      if (this.settings == null) {
         throw new Exception("OptimizationEngine was not initialized!");
      }

      int var1 = this.verifySettings();
      this.initOptimization();
      this.actualStep = 0;
      this.optimizationProfile = new OptimizationProfile();
      if (var1 != 0 && this.optimizationSettings.type != 1) {
         int var2 = 0;
         if (this.optimizationSettings.optimizationType == 0 || this.optimizationSettings.optimizationType == 1) {
            var2 = this.optimizeSimple(true);
         }

         if (var2 > 0) {
            if (var2 < 50) {
               char var4 = '썐';
            }

            this.customStatsComputer = new StatsComputer();
            ArrayList var3 = this.recognizeStatValues();
            this.fitnessStatsComputer = new StatsComputer(var3);
            if (this.wfoSimulationEngine != null) {
               this.wfoSimulationEngine.sortVariants();
            }

            if (this.optimizationSettings.type == 2) {
               this.optimizeWF(
                  null, this.optimizationSettings.periodType, 2, this.optimizationSettings.param1Start, this.optimizationSettings.param2Start, 0, null, false
               );
            } else if (this.optimizationSettings.type == 3) {
               this.optimizeMultiWF();
            }
         }
      } else {
         this.printLog(L.t("Running simple optimization on %s", new Object[]{this.settings.getString("StrategyName")}));
         this.printLog(L.t("Testing original strategy", new Object[0]));
         this.optimizeSimple(false);
      }

      this.clearOptimizationProfile();
   }

   private ArrayList<DatabankColumn> recognizeStatValues() throws Exception {
      ArrayList var1 = this.fitnessFunction.getUsedStatValues();

      for (DatabankColumn var3 : DatabankColumns.get().getAvailableClasses()) {
         if (var3.getClassName().equals("NumberOfTrades") || var3.getClassName().equals("Drawdown")) {
            this.addToStatValues(var1, var3);
         }

         if (this.optimizationSettings.periodType == 30 && var3.getClassName().equals("DrawdownPct")) {
            this.addToStatValues(var1, var3);
         }
      }

      StatsComputer.createDependencyGraph(var1);
      return var1;
   }

   private void addToStatValues(ArrayList<DatabankColumn> var1, DatabankColumn var2) {
      String var3 = var2.getClassName();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         if (((DatabankColumn)var1.get(var4)).getClassName().equals(var3)) {
            return;
         }
      }

      var1.add(var2);
   }

   private int verifySettings() throws BadOptimizationParamsException {
      int var1 = this.parameters.verify();
      this.optimizationSettings.verify();
      return var1;
   }

   private int optimizeSimple(boolean var1) throws Exception {
      return this.runSimpleOptimization(0, 0L, 0L, this.optimizationSettings.type, var1);
   }
}
