package com.strategyquant.tradinglib.optimization;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.backtest.TradingOptionsModifier;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import java.io.Serializable;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationTestJob extends GridJob<BacktestResult> {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationTestJob");
   private double initialCapital;
   private ChartSetups chartSetups;
   private Element elStrategy;
   private Element lastSettings;
   private MoneyManagementMethod moneyManagementMethod;
   private RiskManagementMethod riskManagementMethod;
   private int testPrecision;
   private IStrategyModifier strategyModifier;
   private String optimizationParams;
   private String resultGroupName;
   private short[] optimizationParamIndexes;
   private IFitnessFunction fitnessFunction;
   private TradingOptions tradingOptions;
   private double slippage;
   private double minDistance;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private ATM atm;
   private String mainResultKey;
   private StopPauseEngine parentStopPauseEngine;
   private final int periodType;

   public OptimizationTestJob(String var1, Map<String, Serializable> var2, StopPauseEngine var3, int var4) {
      super(var1, 1, var2);
      this.initialCapital = (Double)((Serializable)var2.get("MoneyManagement.InitialCapital"));
      this.testPrecision = (Integer)((Serializable)var2.get("TestPrecision"));
      this.chartSetups = (ChartSetups)var2.get("ChartSetups");
      this.slippage = (Double)((Serializable)var2.get("Slippage"));
      this.minDistance = (Double)((Serializable)var2.get("MinDistance"));
      this.commission = (CommissionsMethod)var2.get("Commission");
      this.swap = (SwapMethod)var2.get("Swap");
      this.elStrategy = (Element)var2.get("StrategyXml");
      this.lastSettings = (Element)var2.get("StrategyLastSettings");
      this.strategyModifier = (IStrategyModifier)var2.get("StrategyModifier");
      this.optimizationParams = (String)var2.get("OptimizationParams");
      this.optimizationParamIndexes = (short[])var2.get("OptimizationParamIndexes");
      this.moneyManagementMethod = (MoneyManagementMethod)var2.get("MoneyManagement.Method");
      this.riskManagementMethod = (RiskManagementMethod)var2.get("RiskManagement");
      this.resultGroupName = (String)var2.get("ResultGroupName");
      this.mainResultKey = (String)var2.get("MainResultKey");
      this.fitnessFunction = (IFitnessFunction)var2.get("FitnessFunction");
      this.tradingOptions = (TradingOptions)var2.get("TradingOptions");
      this.atm = (ATM)var2.get("ATM");
      this.lastSettings = this.lastSettings != null ? this.lastSettings.clone() : null;
      this.periodType = var4;

      try {
         TradingOptionsModifier.modifyTradingOptions(this.tradingOptions, this.elStrategy, this.lastSettings);
      } catch (Exception var6) {
         Log.error("Cannot modify trading options", var6);
      }

      this.parentStopPauseEngine = var3;
   }

   public BacktestResult call() throws Exception {
      DurationStats var1 = new DurationStats();

      try {
         long var2 = System.currentTimeMillis();
         ResultsGroup var6 = this.backtestStrategy(this.elStrategy, this.chartSetups.get(0));
         var1.addDuration(DurationStats.MainTest, System.currentTimeMillis() - var2);
         var6.setName(this.resultGroupName);
         return new BacktestResult(var6, var1);
      } catch (TradingException var5) {
         if (var5 instanceof BadStrategyException) {
            BadStrategyException var3 = (BadStrategyException)var5;
            int var4 = var3.getReason();
            return var4 == 32
               ? new BacktestResult(L.t("dismissed, too many open trades", new Object[]{var4}), var4, var1)
               : new BacktestResult(L.t("dismissed", new Object[0]) + String.format(" (%d)", var4), var4, var1);
         } else {
            return new BacktestResult(L.t("Exception in backtest", new Object[0]) + ": " + var5.getMessage(), 10001, var1);
         }
      }
   }

   private ResultsGroup backtestStrategy(Element var1, ChartSetup var2) throws Exception {
      String var4 = null;
      if (this.lastSettings != null) {
         var4 = XMLUtil.elementToString(this.lastSettings);
      }

      ResultsGroup var3;
      if (var2.getBacktestEngine() != 1316847364 && var2.getBacktestEngine() != -1816889229) {
         BacktestEngine var8 = new BacktestEngine(TraderSimulators.getSimulator(var2));
         var8.setStopPauseEngine(this.parentStopPauseEngine);
         SettingsMap var11 = this.getBacktestSettings(var1, var2);
         var8.addSetup(var11);
         boolean var12 = this.periodType == 30;
         var3 = var8.runBacktest(this.resultGroupName, this.mainResultKey, false, var12).getResults();
      } else {
         StockpickerBacktestEngine var5 = new StockpickerBacktestEngine();
         var5.setStopPauseEngine(this.parentStopPauseEngine);
         if (var2.getBacktestEngine() == -1816889229) {
            String var6 = var2.getMainChart().getSymbol();
            String var7 = StockpickerBacktestEngine.createBasketName(var6);
            var2 = var2.getClone(var7);
         }

         SettingsMap var10 = this.getBacktestSettings(var1, var2);
         var5.addSetup(var10);
         var5.addStrategyXml(var1);
         var5.setLastSettings(var4);
         var3 = var5.runBacktest(this.resultGroupName);
      }

      var3.specialValues().setString("OptimizationParameters", this.optimizationParams);
      var3.specialValues().set("OptimizationParametersArray", this.optimizationParamIndexes);
      if (var4 != null) {
         var3.setLastSettings(var4);
      }

      int var9 = var3.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
      this.computeFitness(var3);
      return var3;
   }

   private void computeFitness(ResultsGroup var1) throws Exception {
      this.computeFitness(var1, (byte)0, (byte)127);
      this.computeFitness(var1, (byte)0, (byte)10);
      this.computeFitness(var1, (byte)0, (byte)20);
   }

   private void computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      double var4 = this.fitnessFunction.computeFitness(var1, var2, var3);
      if (this.periodType == 30) {
         var4 = this.fitnessFunction.computeFitness(var1, var2, var3, false);
      } else {
         var4 = this.fitnessFunction.computeFitness(var1, var2, var3);
      }

      var1.portfolio().setFitness(var3, var4);
   }

   private SettingsMap getBacktestSettings(Element var1, ChartSetup var2) throws Exception {
      SettingsMap var3 = new SettingsMap();
      var3.set("BacktestChart", var2);
      var3.set("Slippage", this.slippage);
      var3.set("MinDistance", this.minDistance);
      var3.set("Commission", this.commission);
      var3.set("Swap", this.swap);
      var3.set("MoneyManagement.InitialCapital", this.initialCapital);
      var3.set("MoneyManagement.Method", this.moneyManagementMethod);
      var3.set("RiskManagement", this.riskManagementMethod);
      var3.set("StrategyObject", StrategyBase.createXmlStrategy(var1));
      var3.set("ResultGroupName", this.resultGroupName);
      var3.set("TradingOptions", this.tradingOptions);
      var3.set("ATM", this.atm);
      var3.set("TestPrecision", this.testPrecision);
      return var3;
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 4) {
         this.destroy();
      }
   }
}
