package com.strategyquant.plugin.Task.impl.AutomaticRetest;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtest.BacktestDataInitializer;
import com.strategyquant.tradinglib.backtest.LoadDataProgressEngine;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticRetestJob extends GridJob<BacktestResult> {
   public static final Logger Log = LoggerFactory.getLogger("RetestJob");
   private BacktestRunner backtestRunner;
   private SQProject project;
   private String strategyName;
   private Map<String, Serializable> params;

   public AutomaticRetestJob(String var1, Map<String, Serializable> var2, StopPauseEngine var3, ILastEventListener var4) throws Exception {
      super(var1, 1, var2);
      this.params = var2;
      this.backtestRunner = new BacktestRunner(
         new BacktestSettings(var2), var3, this, true, var4, XMLUtil.xmlToString((Element)var2.get("StrategyLastSettings"))
      );

      try {
         this.strategyName = (String)var2.get("StrategyName");
         Databank var5 = (Databank)var2.get("DatabankSource");
         this.project = ProjectEngine.get(var5.getProject());
      } catch (Exception var6) {
      }
   }

   private void initializeBacktestData(Map<String, Serializable> var1) throws Exception {
      ArrayList var2 = (ArrayList)var1.get("ChartSetups");
      boolean var3 = SettingsMap.getBool(var1.get("UseRobustnessTests"), false);
      ArrayList var4 = (ArrayList)var1.get("CrossChecks");
      BacktestDataInitializer var5 = new BacktestDataInitializer(new LoadDataProgressEngine(this.project.progressEngine), var2, var3, var4);
      var5.prepareData();
   }

   public BacktestResult call() throws Exception {
      try {
         this.initializeBacktestData(this.params);
         BacktestResult var1 = this.backtestRunner.execute();

         try {
            ResultsGroup var2 = var1.getResult();
            var2.setLastSettings(XMLUtil.xmlToString((Element)this.params.get("StrategyLastSettings")));
         } catch (Exception var3) {
         }

         return var1;
      } catch (Exception var4) {
         if (this.project != null) {
            this.project.printToLog(L.t("Automatic Backtest of '%s' failed - %s", new Object[]{this.strategyName, var4.getMessage()}));
         }

         throw var4;
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.backtestRunner.stop();
      }
   }
}
