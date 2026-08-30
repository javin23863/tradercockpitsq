package com.strategyquant.tradinglib.backtest;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQBacktesterJob extends GridJob<BacktestResult> {
   public static final Logger Log = LoggerFactory.getLogger("SQBacktesterJob");
   private BacktestRunner backtestRunner;
   private AWProgressEngine progressEngine;
   private BacktestDataInitializer bdi;
   private String strategyName;
   private Element settingsXML;
   private ILastEventListener lastEventListener;

   public SQBacktesterJob(String var1, Map<String, Serializable> var2, ILastEventListener var3) throws Exception {
      super(var1, 1, var2);
      this.lastEventListener = var3;
      if (this.lastEventListener == null) {
         this.lastEventListener = new ILastEventListener() {
            @Override
            public void setLastEvent(String var1) {
            }
         };
      }

      this.progressEngine = new AWProgressEngine();
      this.bdi = new BacktestDataInitializer(new LoadDataProgressEngine(this.progressEngine), (ArrayList<ChartSetup>)var2.get("ChartSetups"), false, null, var3);
      this.backtestRunner = new BacktestRunner(new BacktestSettings(var2), this.progressEngine, this, false, this.lastEventListener, null);
      this.strategyName = (String)var2.get("StrategyName");
      this.settingsXML = (Element)var2.get("StrategyLastSettings");
   }

   public BacktestResult call() throws Exception {
      try {
         this.progressEngine.start();
         long var1 = this.bdi.prepareData();
         BacktestResult var3 = this.backtestRunner.execute();
         ResultsGroup var4 = var3.getResult();
         if (var4 != null) {
            var4.setLastSettings(XMLUtil.elementToString(this.settingsXML));
            var4.specialValues().set(SpecialValues.LoadDataDuration, SQUtils.round(var1 / 1000.0, 2));
         }

         return var3;
      } catch (Exception var5) {
         Log.error("Backtesting strategy '" + this.strategyName + "' failed");
         throw var5;
      }
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 5 && var1.getCustomID() != null && var1.getCustomID().equals("StopBacktest")) {
         this.progressEngine.stop();
         this.backtestRunner.stop();
      }
   }
}
