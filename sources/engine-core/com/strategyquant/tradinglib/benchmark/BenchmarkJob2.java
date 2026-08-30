package com.strategyquant.tradinglib.benchmark;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.backtest.BacktestDataCache;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.robustnesstests.RobustnessTestMethod;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkJob2 extends GridJob<ResultsGroup> {
   public static final Logger Log = LoggerFactory.getLogger("BenchmarkJob");
   private int initialCapital;
   private OutOfSample outOfSample;
   private ChartSetups chartSetups;
   private String strategyName;
   private ArrayList<TradingOption> tradingOptions;
   private Element elStrategy;
   private MoneyManagementMethod moneyManagementMethod;
   private RiskManagementMethod riskManagementMethod;
   private IFitnessFunction fitnessFunction;
   private boolean useRobustnessTests;
   private ArrayList<RobustnessTestMethod> rtMethods;
   private int rtNumberOfSimulations;
   private boolean rtForEverySetup;
   private SettingsMap backtestSettings = null;
   private ObjectArrayList<VersatileData> bigArray;

   public BenchmarkJob2(String var1, Map<String, Serializable> var2) {
      super(var1, 1, var2);
      this.strategyName = (String)var2.get("StrategyName");
      this.elStrategy = (Element)var2.get("StrategyXml");
      this.moneyManagementMethod = (MoneyManagementMethod)var2.get("MoneyManagement.Method");
      this.riskManagementMethod = (RiskManagementMethod)var2.get("RiskManagement");
      this.fitnessFunction = (IFitnessFunction)var2.get("FitnessFunction");
      this.tradingOptions = (ArrayList<TradingOption>)var2.get("TradingOptions");
      this.initialCapital = (Integer)((Serializable)var2.get("MoneyManagement.InitialCapital"));
      this.outOfSample = (OutOfSample)var2.get("OutOfSample");
      this.chartSetups = (ChartSetups)var2.get("ChartSetups");
      this.useRobustnessTests = (Boolean)((Serializable)var2.get("UseRobustnessTests"));
      this.rtMethods = (ArrayList<RobustnessTestMethod>)var2.get("RTMethods");
      this.rtNumberOfSimulations = (Integer)((Serializable)var2.get("RTNumberOfSimulations"));
      this.rtForEverySetup = (Boolean)((Serializable)var2.get("RTForEverySetup"));
      this.bigArray = (ObjectArrayList<VersatileData>)var2.get("BigArray");
   }

   public ResultsGroup call() throws Exception {
      long var1 = System.currentTimeMillis();
      BacktestEngine var3 = new BacktestEngine(TraderSimulators.getSimulator(this.chartSetups.getMainSetup()));
      this.backtestSettings = this.getBacktestSettings(this.elStrategy, this.chartSetups.getMainSetup());
      var3.addSetup(this.backtestSettings);
      boolean var4 = false;
      new VersatileData();
      BacktestDataCache var6 = BacktestDataCache.getInstance();
      ArrayList var7 = this.getAllBacktestChartDefs();
      throw new Exception(L.t("Need to implement this below!", new Object[0]));
   }

   public ArrayList<ChartDef> getAllBacktestChartDefs() throws Exception {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = this.chartSetups.getMainSetup().getCharts();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         ChartDef var4 = (ChartDef)var2.get(var3);
         var1.add(new ChartDef(var4));
      }

      return var1;
   }

   private SettingsMap getBacktestSettings(Element var1, ChartSetup var2) throws Exception {
      SettingsMap var3 = new SettingsMap();
      var3.set("ChartSetups", this.chartSetups);
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

   public Integer call2() throws Exception {
      long var1 = System.currentTimeMillis();
      int var3 = 0;

      for (int var4 = 0; var4 < 90000; var4++) {
         for (int var5 = 0; var5 < 30000; var5++) {
            var3 += var4 + var5;
         }
      }

      long var6 = System.currentTimeMillis();
      Log.info(String.format("CALL TOOK: %d ms", var6 - var1));
      return var3;
   }
}
