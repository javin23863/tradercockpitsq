package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.backtest.TradingOptionsModifier;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IFitnessEvaluator;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizerBacktestEvaluator implements IFitnessEvaluator<OptimizationIndexes> {
   private static final Logger Log = LoggerFactory.getLogger("OptimizerBacktestEvaluator");
   private BacktestSettings backtestSettings;
   private BacktestRunner backtestRunner;
   private StopPauseEngine parentStopPauseEngine;
   private OptimizationSettings optimizationSettings;
   private ILastEventListener lastEventListener;
   private final String lastSettingsXml;

   public OptimizerBacktestEvaluator(BacktestSettings var1, OptimizationSettings var2, StopPauseEngine var3, ILastEventListener var4, String var5) {
      this.backtestSettings = var1;
      this.parentStopPauseEngine = var3;
      this.optimizationSettings = var2;
      this.lastEventListener = var4;
      this.lastSettingsXml = var5;
   }

   public OptimizationIndexes evaluateCandidate(OptimizationIndexes var1, int var2, GridJob var3, boolean var4) {
      try {
         GPIDs var5 = var1.getGPIDs();
         String var27 = String.format("%s - Optimization %d.%d.%d", this.backtestSettings.strategyName, var5.islandIndex, var5.generationIndex, var5.nodeIndex);
         Log.debug("--- Job {} started", var27);
         StrategyBase var7 = StrategyBase.createXmlStrategy(this.backtestSettings.elStrategy, "temp");
         StrategyParamData var8 = StrategyBase.createStrategyVariation(
            var7, this.optimizationSettings, this.optimizationSettings.transformToFullIndexes(var1.getIndexes())
         );
         StrategyBase var9 = var8.getStrategy();
         String var10 = var8.getParams();
         Element var11 = var9.getStrategyXml();
         Element var12 = this.backtestSettings.lastSettings != null ? this.backtestSettings.lastSettings.clone() : null;

         try {
            TradingOptionsModifier.modifyTradingOptions(this.backtestSettings.tradingOptions, var11, var12);
         } catch (Exception var24) {
            Log.error("Cannot modify trading options", var24);
         }

         this.backtestRunner = new BacktestRunner(this.backtestSettings, this.parentStopPauseEngine, var3, false, this.lastEventListener, this.lastSettingsXml);
         this.backtestRunner.setStrategyName(var27);
         this.backtestRunner.setStrategy(var11);
         BacktestResult var13 = this.backtestRunner.execute(var2);
         int var14 = var13.getDismissalReason();
         String var15 = var13.getDismissalMessage();
         if (var15 != null) {
            var1.setError(var14, var15, null);
            return var1;
         }

         ResultsGroup var16 = var13.getResult();
         if (var16 == null) {
            var1.setError(10001, "No dismissal message, but ResultsGroup was null in BacktestResult", null);
            Log.error("No dismissal message, but ResultsGroup was null in BacktestResult");
            return var1;
         }

         double var17 = var16.portfolio().getFitness((byte)10);
         var16.specialValues().setString("OptimizationParameters", var10);
         var16.specialValues().set("OptimizationParametersArray", var1.getIndexes());
         if (var12 != null) {
            var16.setLastSettings(XMLUtil.elementToString(var12));
         }

         var1.setFitness(var17, (byte)0);
         var1.setResult(var16);
         return var1;
      } catch (Exception var25) {
         String var6 = var25.getMessage();
         if (var6 == null) {
            Log.info("Exception in optimizer backtest", var25);
            var1.setError(10001, String.format("Exception in backtest: NullPointerException", var25.getMessage()), var25);
         } else if (!var6.contains("Index out of range")
            && !var6.contains("Undefined type:")
            && !var6.contains("Invalid MA type used")
            && !var6.contains("Invalid PriceField value")
            && !var6.contains("Invalid Signal Strength")) {
            Log.info("Exception in optimizer backtest " + var6, var25);
            var1.setError(10001, String.format("Exception in backtest: %s", var25.getMessage()), var25);
         } else {
            var1.setError(10013, String.format("Exception in backtest: %s", var25.getMessage()), var25);
         }

         return var1;
      } finally {
         ;
      }
   }

   public OptimizerBacktestEvaluator getClone() throws Exception {
      return new OptimizerBacktestEvaluator(
         this.backtestSettings.getClone(), this.optimizationSettings, this.parentStopPauseEngine, this.lastEventListener, this.lastSettingsXml
      );
   }

   @Override
   public void destroy() {
      if (this.backtestRunner != null) {
         this.backtestRunner.stop();
      }
   }

   @Override
   public void stop() {
   }
}
