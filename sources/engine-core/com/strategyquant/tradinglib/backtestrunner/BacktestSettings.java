package com.strategyquant.tradinglib.backtestrunner;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.correlation.FitPortfolio;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

public class BacktestSettings implements ISQCloneable {
   public double initialCapital;
   public MoneyManagementMethod moneyManagementMethod;
   public boolean moneyManagementMethodUseFromStrategy;
   public RiskManagementMethod riskManagementMethod;
   public IFitnessFunction fitnessFunction;
   public ArrayList<Condition> rankingConditions;
   public ArrayList<Condition> initialGenerationConditions;
   public TradingOptions tradingOptions;
   public ChartSetups chartSetups;
   public OutOfSample outOfSample;
   public boolean useCrossChecks;
   public ArrayList<ICrossCheck> crossChecks;
   public boolean evaluateAllCrossChecks = false;
   public boolean forceRunCrossChecks = false;
   public double slippage;
   public double minDistance;
   public CommissionsMethod commission;
   public SwapMethod swap;
   public String strategyName;
   public Element elStrategy;
   public Element lastSettings;
   public int dismissBadStrategies = 0;
   public StrategyBase strategyObject;
   public String mainResultKey;
   public boolean deleteFailedStrategies = false;
   public boolean isRetester = false;
   public boolean singleThreadedOptimizations = true;
   public List<ResultsGroup> mergedStrategies = null;
   public long dateGenerated;
   public boolean warningsBadStrategies = false;
   public ATM atm;
   public FitPortfolio fitPortfolio;
   public boolean isAlgoWizard = false;
   public boolean removeEndOfTestOrders = false;
   public String userID = null;
   public String groupSymbols = null;
   public String brokerSymbols = null;

   public BacktestSettings() {
   }

   public BacktestSettings(Map<String, Serializable> var1) throws Exception {
      this.strategyName = (String)var1.get("StrategyName");
      this.mainResultKey = (String)var1.get("MainResultKey");
      this.elStrategy = (Element)var1.get("StrategyXml");
      this.lastSettings = (Element)var1.get("StrategyLastSettings");
      this.moneyManagementMethod = (MoneyManagementMethod)var1.get("MoneyManagement.Method");
      if (this.moneyManagementMethod != null) {
         this.moneyManagementMethod = this.moneyManagementMethod.getClone();
      }

      this.moneyManagementMethodUseFromStrategy = SettingsMap.getBool(var1.get("MoneyManagement.UseFromStrategy"), false);
      this.riskManagementMethod = (RiskManagementMethod)var1.get("RiskManagement");
      if (this.riskManagementMethod != null) {
         this.riskManagementMethod = this.riskManagementMethod.getClone();
      }

      this.fitnessFunction = (IFitnessFunction)var1.get("FitnessFunction");
      this.tradingOptions = (TradingOptions)var1.get("TradingOptions");
      this.atm = (ATM)var1.get("ATM");
      this.initialCapital = (Double)((Serializable)var1.get("MoneyManagement.InitialCapital"));
      this.outOfSample = (OutOfSample)var1.get("OutOfSample");
      this.chartSetups = (ChartSetups)var1.get("ChartSetups");
      this.useCrossChecks = SettingsMap.getBool(var1.get("UseRobustnessTests"), false);
      this.crossChecks = (ArrayList<ICrossCheck>)var1.get("CrossChecks");
      this.slippage = SettingsMap.getDouble(var1.get("Slippage"), 0.0);
      this.minDistance = SettingsMap.getDouble(var1.get("MinDistance"), 0.0);
      this.commission = (CommissionsMethod)var1.get("Commission");
      this.swap = (SwapMethod)var1.get("Swap");
      this.rankingConditions = (ArrayList<Condition>)var1.get("Conditions");
      this.dismissBadStrategies = SettingsMap.getInt(var1.get("DismissBadStrategies"), 0);
      this.warningsBadStrategies = SettingsMap.getBool(var1.get("StrategyDismissWarnings"), false);
      this.strategyObject = (StrategyBase)var1.get("StrategyObject");
      this.evaluateAllCrossChecks = SettingsMap.getBool(var1.get("CrossChecks.EvaluateAll"), false);
      this.deleteFailedStrategies = SettingsMap.getBool(var1.get("DeleteFailedStrategies"), false);
      this.forceRunCrossChecks = SettingsMap.getBool(var1.get("ForceRunCrossChecks"), false);
      this.isRetester = SettingsMap.getBool(var1.get("IsRetester"), false);
      this.singleThreadedOptimizations = SettingsMap.getBool(var1.get("SingleThreadedOptimizations"), true);
      this.mergedStrategies = (List<ResultsGroup>)var1.get("PortfolioMergedStrategies");
      this.dateGenerated = SettingsMap.getLong(var1.get("DateGenerated"), -1L);
      this.fitPortfolio = (FitPortfolio)var1.get("FitPortfolio");
      this.isAlgoWizard = SettingsMap.getBool(var1.get("IsAlgoWizard"), false);
      this.userID = (String)var1.get("UserID");
      this.brokerSymbols = (String)var1.get("BrokerSymbols");
   }

   public BacktestSettings(SettingsMap var1) throws Exception {
      this.strategyName = (String)var1.get("StrategyName");
      this.mainResultKey = (String)var1.get("MainResultKey");
      this.elStrategy = (Element)var1.get("StrategyXml");
      this.lastSettings = (Element)var1.get("StrategyLastSettings");
      this.strategyObject = (StrategyBase)var1.get("StrategyObject");
      this.moneyManagementMethod = (MoneyManagementMethod)var1.get("MoneyManagement.Method");
      if (this.moneyManagementMethod != null) {
         this.moneyManagementMethod = this.moneyManagementMethod.getClone();
      }

      this.moneyManagementMethodUseFromStrategy = SettingsMap.getBool(var1.get("MoneyManagement.UseFromStrategy"), false);
      this.riskManagementMethod = (RiskManagementMethod)var1.get("RiskManagement");
      if (this.riskManagementMethod != null) {
         this.riskManagementMethod = this.riskManagementMethod.getClone();
      }

      this.fitnessFunction = (IFitnessFunction)var1.get("FitnessFunction");
      this.tradingOptions = (TradingOptions)var1.get("TradingOptions");
      this.atm = (ATM)var1.get("ATM");
      this.initialCapital = (Double)var1.get("MoneyManagement.InitialCapital");
      this.outOfSample = (OutOfSample)var1.get("OutOfSample");
      if (var1.containsKey("ChartSetups")) {
         this.chartSetups = (ChartSetups)var1.get("ChartSetups");
         this.useCrossChecks = (Boolean)var1.get("UseRobustnessTests", false);
         this.crossChecks = (ArrayList<ICrossCheck>)var1.get("CrossChecks");
         this.slippage = (Double)var1.get("Slippage");
         this.minDistance = (Double)var1.get("MinDistance");
         this.commission = (CommissionsMethod)var1.get("Commission");
         this.swap = (SwapMethod)var1.get("Swap");
         this.rankingConditions = (ArrayList<Condition>)var1.get("Conditions", null);
         this.dismissBadStrategies = (Integer)var1.get("DismissBadStrategies", 0);
         this.warningsBadStrategies = SettingsMap.getBool(var1.get("StrategyDismissWarnings"), false);
         this.evaluateAllCrossChecks = SettingsMap.getBool(var1.get("CrossChecks.EvaluateAll"), false);
         this.deleteFailedStrategies = SettingsMap.getBool(var1.get("DeleteFailedStrategies"), false);
         this.forceRunCrossChecks = SettingsMap.getBool(var1.get("ForceRunCrossChecks"), false);
         this.isRetester = SettingsMap.getBool(var1.get("IsRetester"), false);
         this.singleThreadedOptimizations = SettingsMap.getBool(var1.get("SingleThreadedOptimizations"), true);
         this.mergedStrategies = (List<ResultsGroup>)var1.get("PortfolioMergedStrategies");
         this.dateGenerated = var1.getLong("DateGenerated", -1L);
         this.fitPortfolio = (FitPortfolio)var1.get("FitPortfolio");
         this.isAlgoWizard = SettingsMap.getBool(var1.get("IsAlgoWizard"), false);
         this.userID = (String)var1.get("UserID");
         this.brokerSymbols = (String)var1.get("BrokerSymbols");
      } else {
         throw new Exception(L.t("Settings don't have ChartSetups defined", new Object[0]));
      }
   }

   public BacktestSettings getClone() throws Exception {
      BacktestSettings var1 = new BacktestSettings();
      var1.initialCapital = this.initialCapital;
      var1.moneyManagementMethod = this.moneyManagementMethod != null ? this.moneyManagementMethod.getClone() : null;
      var1.moneyManagementMethodUseFromStrategy = this.moneyManagementMethodUseFromStrategy;
      var1.riskManagementMethod = this.riskManagementMethod != null ? this.riskManagementMethod.getClone() : null;
      var1.fitnessFunction = this.fitnessFunction;
      var1.tradingOptions = this.tradingOptions != null ? this.tradingOptions.getClone() : null;
      var1.atm = this.atm != null ? this.atm.getClone() : null;
      var1.rankingConditions = this.cloneConditions(this.rankingConditions);
      var1.initialGenerationConditions = this.cloneConditions(this.initialGenerationConditions);
      var1.dismissBadStrategies = this.dismissBadStrategies;
      var1.warningsBadStrategies = this.warningsBadStrategies;
      var1.outOfSample = this.outOfSample;
      var1.chartSetups = this.chartSetups;
      var1.useCrossChecks = this.useCrossChecks;
      if (this.useCrossChecks) {
         var1.crossChecks = ProjectConfigHelper.cloneCrossChecks(this.crossChecks);
      }

      var1.slippage = this.slippage;
      var1.minDistance = this.minDistance;
      var1.commission = this.commission;
      var1.swap = this.swap;
      var1.elStrategy = this.elStrategy;
      var1.lastSettings = this.lastSettings != null ? this.lastSettings.clone() : null;
      var1.strategyName = this.strategyName;
      var1.mainResultKey = this.mainResultKey;
      var1.isRetester = this.isRetester;
      var1.singleThreadedOptimizations = this.singleThreadedOptimizations;
      var1.mergedStrategies = this.mergedStrategies;
      var1.dateGenerated = this.dateGenerated;
      var1.fitPortfolio = this.fitPortfolio != null ? this.fitPortfolio.getClone() : null;
      var1.isAlgoWizard = this.isAlgoWizard;
      var1.removeEndOfTestOrders = this.removeEndOfTestOrders;
      var1.userID = this.userID;
      var1.groupSymbols = this.groupSymbols;
      var1.brokerSymbols = this.brokerSymbols;
      return var1;
   }

   private ArrayList<Condition> cloneConditions(ArrayList<Condition> var1) throws Exception {
      if (var1 == null) {
         return null;
      }

      ArrayList var2 = new ArrayList();

      for (Condition var4 : var1) {
         var2.add(var4.getClone());
      }

      return var2;
   }

   public SettingsMap getSettingsMap(ChartSetup var1) throws Exception {
      SettingsMap var2 = new SettingsMap();
      var2.set("BacktestChart", var1);
      var2.set("MoneyManagement.InitialCapital", this.initialCapital);
      var2.set("OutOfSample", this.outOfSample);
      var2.set("MoneyManagement.Method", this.moneyManagementMethod);
      var2.set("MoneyManagement.UseFromStrategy", this.moneyManagementMethodUseFromStrategy);
      var2.set("RiskManagement", this.riskManagementMethod);
      if (this.strategyObject == null) {
         var2.set("StrategyObject", StrategyBase.createXmlStrategy(this.elStrategy.clone(), this.strategyName));
      } else {
         var2.set("StrategyObject", this.strategyObject);
      }

      var2.set("Slippage", this.slippage);
      var2.set("MinDistance", this.minDistance);
      var2.set("Commission", this.commission);
      var2.set("Swap", this.swap);
      var2.set("StrategyName", this.strategyName);
      var2.set("MainResultKey", this.mainResultKey);
      long var3 = var1.getMainChart().getHistoryFrom() + (long)((var1.getMainChart().getHistoryTo() - var1.getMainChart().getHistoryFrom()) * 0.4);
      var2.set("StrategyCheckDate", var3);
      var2.set("DismissBadStrategies", this.dismissBadStrategies);
      var2.set("StrategyDismissWarnings", this.warningsBadStrategies);
      var2.set("IsRetester", this.isRetester);
      var2.set("IsAlgoWizard", this.isAlgoWizard);
      var2.set("UserID", this.userID);
      var2.set("SingleThreadedOptimizations", this.singleThreadedOptimizations);
      if (this.tradingOptions != null) {
         var2.set("TradingOptions", this.tradingOptions);
      }

      if (this.atm != null) {
         var2.set("ATM", this.atm);
      }

      if (this.fitPortfolio != null) {
         var2.set("FitPortfolio", this.fitPortfolio);
      }

      var2.set("BrokerSymbols", this.brokerSymbols);
      return var2;
   }
}
