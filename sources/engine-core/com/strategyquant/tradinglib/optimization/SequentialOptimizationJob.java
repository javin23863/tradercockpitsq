package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequentialOptimizationJob extends GridJob<BacktestResult> {
   public static final Logger Log = LoggerFactory.getLogger("ChainOptimizationJob");
   private BacktestRunner backtestRunner;
   private String note;
   private ILastEventListener lastEventListener;
   private String strategyName;
   private String paramName;
   private double paramValue;

   public SequentialOptimizationJob(
      String var1, Map<String, Serializable> var2, StopPauseEngine var3, ILastEventListener var4, String var5, String var6, double var7, String var9
   ) throws Exception {
      super(var1, 1, var2);
      this.strategyName = var5;
      this.paramName = var6;
      this.paramValue = var7;
      this.lastEventListener = var4;
      this.backtestRunner = new BacktestRunner(new BacktestSettings(var2), var3, null, false, null, var9);

      try {
         this.note = (String)var2.get("StrategyNote");
      } catch (Exception var11) {
         Log.error("Error while initializing ChainOptimization job", var11);
      }
   }

   public BacktestResult call() throws Exception {
      try {
         if (this.lastEventListener != null) {
            String var1 = L.t("%s: Sequential optimization - %s = %s", new Object[]{this.strategyName, this.paramName, "" + this.paramValue});
            this.lastEventListener.setLastEvent(var1);
         }

         BacktestResult var4 = this.backtestRunner.execute();
         if (var4 != null && this.note != null) {
            ResultsGroup var2 = var4.getResult();
            if (var2 != null) {
               var2.specialValues().set(SpecialValues.Note, this.note);
            }
         }

         return var4;
      } catch (Exception var3) {
         throw var3;
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.backtestRunner.stop();
      }
   }
}
