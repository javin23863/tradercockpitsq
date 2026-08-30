package com.strategyquant.plugin.Task.impl.Retest;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.io.Serializable;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetestJob extends GridJob<BacktestResult> {
   public static final Logger Log = LoggerFactory.getLogger("RetestJob");
   private BacktestRunner backtestRunner;
   private SQProject project;
   private String strategyName;
   private String note;

   public RetestJob(String var1, Map<String, Serializable> var2, StopPauseEngine var3, ILastEventListener var4, String var5) throws Exception {
      super(var1, 1, var2);
      this.backtestRunner = new BacktestRunner(new BacktestSettings(var2), var3, this, true, var4, var5);

      try {
         this.strategyName = (String)var2.get("StrategyName");
         this.note = (String)var2.get("StrategyNote");
         Databank var6 = (Databank)var2.get("DatabankSource");
         this.project = ProjectEngine.get(var6.getProject());
      } catch (Exception var7) {
         Log.error("Error while initializing Retest job", var7);
      }
   }

   public BacktestResult call() throws Exception {
      try {
         BacktestResult var1 = this.backtestRunner.execute();
         if (var1 != null && this.note != null) {
            ResultsGroup var2 = var1.getResult();
            if (var2 != null) {
               var2.specialValues().set(SpecialValues.Note, this.note);
            }
         }

         return var1;
      } catch (Exception var3) {
         if (this.project != null) {
            this.project.printToLog(L.t("Backtest of '%s' failed - %s", new Object[]{this.strategyName, var3.getMessage()}));
         }

         throw var3;
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.backtestRunner.stop();
      }
   }
}
