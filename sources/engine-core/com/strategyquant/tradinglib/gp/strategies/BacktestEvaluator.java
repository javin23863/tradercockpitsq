package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IFitnessEvaluator;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestEvaluator implements IFitnessEvaluator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("BacktestEvaluator");
   private BacktestSettings backtestSettings;
   private BacktestRunner backtestRunner;
   private ILastEventListener lastEventListener;
   private final String lastSettingsXml;

   public BacktestEvaluator(BacktestSettings var1, ILastEventListener var2, String var3) {
      this.backtestSettings = var1;
      this.lastEventListener = var2;
      this.lastSettingsXml = var3;
   }

   public Node evaluateCandidate(Node var1, int var2, GridJob var3, boolean var4) {
      Element var5 = null;

      try {
         GPIDs var6 = var1.getGPIDs();
         if (var4) {
         }

         this.backtestSettings.strategyName = this.getStrategyName(var6);
         StopPauseEngine var15 = new StopPauseEngine();
         var15.start();
         this.backtestRunner = new BacktestRunner(this.backtestSettings, var15, var3, false, this.lastEventListener, this.lastSettingsXml);
         String var8 = String.format("Strategy %d.%d.%d", var6.islandIndex + 1, var6.generationIndex, var6.nodeIndex);
         if (Log.isDebugEnabled()) {
            Log.debug("--- Job {} started", var8);
         }

         var5 = var1.getStrategy();
         this.backtestRunner.setStrategyName(var8);
         this.backtestRunner.setStrategy(var5);
         this.backtestRunner.setEvolution(true);
         BacktestResult var9 = this.backtestRunner.execute(var2);
         int var10 = var9.getDismissalReason();
         String var11 = var9.getDismissalMessage();
         var1.setDurationStats(var9.getDurationStats());
         if (var11 != null) {
            var1.setError(var10, var11, null);
         }

         ResultsGroup var12 = var9.getResult();
         if (var12 == null) {
            if (var11 == null) {
               var1.setError(10001, "No dismissal message, but ResultsGroup was null in BacktestResult", null);
            }
         } else {
            var1.setFitness(var12.portfolio().getFitness((byte)10), (byte)10);
            var1.setFitness(var12.portfolio().getFitness((byte)11), (byte)11);
            var1.setFitness(var12.portfolio().getFitness((byte)40), (byte)40);
            var1.setFitness(var12.portfolio().getFitness((byte)20), (byte)20);
            var1.setResultsGroup(var12);
         }

         return var1;
      } catch (Exception var13) {
         if (var13 instanceof TaskStoppedException) {
            Log.info("Task stopped.");
         } else if (!var13.getMessage().contains("Invalid output index:")) {
            Log.error(SQUtils.getStackTrace(var13), var13);
         }

         String var7 = var13.getMessage();
         if (var7 != null && var7.length() < 8) {
            var7 = SQUtils.getStackTrace(var13).substring(0, 100);
         }

         var1.setError(10001, String.format("Exception in backtest: %s", var7), var13);
         return var1;
      }
   }

   private String getStrategyName(GPIDs var1) {
      return String.format("Strategy %d.%d.%d", var1.islandIndex + 1, var1.generationIndex, var1.nodeIndex);
   }

   private void setDismissalToFalse(BacktestSettings var1) {
      this.backtestSettings.dismissBadStrategies = 0;
      if (this.backtestSettings.crossChecks != null && this.backtestSettings.crossChecks.size() != 0) {
         ArrayList var2 = ProjectConfigHelper.cloneCrossChecks(this.backtestSettings.crossChecks);
      }
   }

   public BacktestEvaluator getClone() throws Exception {
      return new BacktestEvaluator(this.backtestSettings.getClone(), this.lastEventListener, this.lastSettingsXml);
   }

   @Override
   public void destroy() {
      if (this.backtestRunner != null) {
         this.backtestRunner.stop();
      }
   }

   @Override
   public void stop() {
      if (this.backtestRunner != null) {
         this.backtestRunner.stop();
      }
   }
}
