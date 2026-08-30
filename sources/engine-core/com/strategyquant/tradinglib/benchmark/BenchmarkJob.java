package com.strategyquant.tradinglib.benchmark;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.simulator.impl.MetaTrader4Simulator;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkJob extends GridJob<ResultsGroup> {
   public static final Logger Log = LoggerFactory.getLogger("BenchmarkJob");
   private double initialCapital;
   private OutOfSample outOfSample;
   private ChartSetups chartSetups;
   private ArrayList<TradingOption> tradingOptions;
   private Element elStrategy;
   private MoneyManagementMethod moneyManagementMethod;
   private RiskManagementMethod riskManagementMethod;
   private SettingsMap backtestSettings = null;

   public BenchmarkJob(String var1, Map<String, Serializable> var2) {
      super(var1, 1, var2);
      this.elStrategy = (Element)var2.get("StrategyXml");
      this.moneyManagementMethod = (MoneyManagementMethod)var2.get("MoneyManagement.Method");
      this.riskManagementMethod = (RiskManagementMethod)var2.get("RiskManagement");
      this.tradingOptions = (ArrayList<TradingOption>)var2.get("TradingOptions");
      this.initialCapital = (Double)((Serializable)var2.get("MoneyManagement.InitialCapital"));
      this.outOfSample = (OutOfSample)var2.get("OutOfSample");
      this.chartSetups = (ChartSetups)var2.get("ChartSetups");
   }

   public ResultsGroup call() throws Exception {
      long var1 = System.currentTimeMillis();
      ResultsGroup var3 = this.backtestStrategy(this.elStrategy, this.chartSetups.getMainSetup());
      int var4 = var3.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
      long var5 = System.currentTimeMillis();
      return var3;
   }

   private ResultsGroup backtestStrategy(Element var1, ChartSetup var2) throws Exception {
      MetaTrader4Simulator var3 = new MetaTrader4Simulator();
      var3.setTestPrecision(var2.getTestPrecision());
      BacktestEngine var4 = new BacktestEngine(var3);
      this.backtestSettings = this.getBacktestSettings(var1, var2);
      var4.addSetup(this.backtestSettings);
      return var4.runBacktest().getResults();
   }

   private SettingsMap getBacktestSettings(Element var1, ChartSetup var2) throws Exception {
      SettingsMap var3 = new SettingsMap();
      var3.set("ChartSetups", this.chartSetups);
      var3.set("BacktestChart", var2);
      var3.set("MoneyManagement.InitialCapital", this.initialCapital);
      var3.set("OutOfSample", this.outOfSample);
      var3.set("MoneyManagement.Method", this.moneyManagementMethod);
      var3.set("RiskManagement", this.riskManagementMethod);
      var3.set("StrategyObject", StrategyBase.createXmlStrategy(var1));

      for (TradingOption var5 : this.tradingOptions) {
         String var6 = var5.getClass().getSimpleName();
         List var7 = var5.getParametersList();

         for (String var9 : var7) {
            String var10;
            if (var7.size() <= 1 && var6.equals(var9)) {
               var10 = var9;
            } else {
               var10 = String.format("%s.%s", var6, var9);
            }

            var3.set(var10, var5.getParameterValue(var9));
         }
      }

      return var3;
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.destroy();
      }
   }
}
