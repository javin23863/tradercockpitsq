package com.strategyquant.plugin.CrossCheck.impl.WalkForwardOptimization;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.OptimizationConst;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginImplementation
public class WalkForwardOptimization extends WalkForwardCrossCheckMethod {
   public static final Logger Log = LoggerFactory.getLogger("WalkForwardOptimizationCrossCheck");
   private int distributionUp = 30;
   private int distributionDown = 30;
   private int maxSteps = 10;
   private ValuesMap paramTypesWFO = new ValuesMap();

   public WalkForwardOptimization() {
      super("WalkForwardOptimization_CrossCheck");
   }

   public String getName() {
      return NameWFOptimization;
   }

   public String getShortName() {
      return L.tsq("WF optim.");
   }

   public String getDescription() {
      return L.tsq(
         "This cross check performs Walk-Forward Optimization of the strategy using the settings below.<br/>Be aware that WF optimization is very slow, it requires repeated backtests of the strategy in different time ranges and using different parameter values."
      );
   }

   public int getPreferredPosition() {
      return 270;
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getParentElement().getParentElement();
      this.elRankings = var3.getChild("Rankings");
      Element var4 = var1.getChild("Settings");
      if (var4 == null) {
         throw new Exception(L.t("Missing Settings element in Crosscheck config", new Object[0]));
      }

      Element var5 = var4.getChild("WalkForward");
      if (var5 == null) {
         throw new Exception(L.t("Missing WalkForward element in Crosscheck config", new Object[0]));
      }

      this.optimizationSettings.optimizationType = XMLUtil.getIntAttr(var5, "type", 1);
      this.optimizationSettings.periodType = XMLUtil.getIntAttr(var5, "period", 10);
      this.optimizationSettings.floating = XMLUtil.getIntAttr(var5, "optimization", 15) == 15;
      this.distributionUp = XMLUtil.getIntAttr(var5, "distributionUp", 30);
      this.distributionDown = XMLUtil.getIntAttr(var5, "distributionDown", 30);
      this.maxSteps = XMLUtil.getIntAttr(var5, "maxSteps", 10);
      Element var6 = var5.getChild("Param1");
      Element var7 = var5.getChild("Param2");
      this.optimizationSettings.param1Start = XMLUtil.getIntAttr(var6, "value", 20);
      this.optimizationSettings.param2Start = XMLUtil.getIntAttr(var7, "value", 10);
      this.optimizationSettings.maxOptimizationBacktests = XMLUtil.getNodeIntValue(var4, "MaxTests", 100);
      this.optimizationSettings.singleThreaded = true;
      Element var8 = var4.getChild("WhatToParametrize");
      this.readWhatToParametrize(var8);
      this.paramTypesWFO = this.paramTypes.clone();
      Element var9 = var1.getChild("AcceptanceSettings");
      if (var9 == null) {
         throw new Exception(L.t("Missing AcceptanceSettings element in Crosscheck config", new Object[0]));
      }

      try {
         Element var10 = XMLUtil.tryAddElement(var9, "Conditions");
         this.optimizationSettings.elConditions = var10;
         this.optimizationSettings.conditions = ProjectConfigHelper.getConditions(var10);
         this.optimizationSettings.thresholdPct = XMLUtil.getIntAttr(var10, "thresholdPct", 10);
      } catch (Exception var11) {
         throw new Exception(L.t("Cannot read conditions of Crosscheck method '%s'", new Object[]{this.getName()}), var11);
      }
   }

   protected SettingsMap prepareSettings(String var1, SettingsMap var2, Element var3, boolean var4) throws Exception {
      try {
         SettingsMap var5 = this.cloneSettings(var2);
         ChartSetups var6 = new ChartSetups();
         var6.add((ChartSetup)var5.get("BacktestChart"));
         var5.set("ChartSetups", var6);
         var5.set("TestPrecision", var2.get("BacktestPrecision"));
         var5.set("StrategyXml", var3);
         StrategyBase var7 = StrategyBase.createXmlStrategy(var3);
         var7.transformToVariables(this.paramSymmetry, this.paramTypesWFO);
         Variables var8 = var7.variables();
         var8.sortByName();
         var5.set("StrategyObject", var7);
         var5.set("StrategyName", var1);
         this.optimizationSettings.parameters = this.initializeParams(var7, this.distributionUp, this.distributionDown, this.maxSteps);
         this.optimizationSettings.optimizationMethod = 0;
         this.optimizationSettings.type = 2;
         this.optimizationSettings.singleThreaded = var4;
         var5.set("OptimizationSettings", this.optimizationSettings.clone());
         IFitnessFunction var9 = ProjectConfigHelper.getFitnessFunction(this.elRankings);
         var5.set("FitnessFunction", var9);
         var5.set("Slippage", var2.get("Slippage"));
         var5.set("MinDistance", var2.get("MinDistance"));
         var5.set("Commission", var2.get("Commission"));
         var5.set("Swap", var2.get("Swap"));
         return var5;
      } catch (Exception var10) {
         Log.error(SQUtils.getStackTrace(var10));
         throw new Exception("Error while preparing settings - " + var10.getMessage());
      }
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      this.checkSpecialTrialLimitation();
      return super.runTest(var1, var2, var3, var5, var6, var7, var8);
   }

   public ICrossCheck clone(SettingsMap var1) {
      WalkForwardOptimization var2 = new WalkForwardOptimization();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      var2.elRankings = this.elRankings;
      var2.optimizationSettings = this.optimizationSettings.clone();
      var2.distributionUp = this.distributionUp;
      var2.distributionDown = this.distributionDown;
      var2.maxSteps = this.maxSteps;
      var2.paramTypesWFO = this.paramTypes.clone();
      return var2;
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      return null;
   }

   public String printSettings(Element var1) throws Exception {
      if (var1 == null) {
         throw new NullPointerException();
      }

      if (!XMLUtil.elementIs(var1, "use")) {
         return "Not used";
      }

      Element var2 = var1.getChild("Settings");
      Element var3 = var2.getChild("WalkForward");
      int var4 = XMLUtil.getIntAttr(var3, "period", 10);
      String var5 = "";
      var5 = OptimizationConst.wfTypeToString(XMLUtil.getIntAttr(var3, "type", 1));
      var5 = var5 + "," + OptimizationConst.getPeriodTypeAsString(var4);
      var5 = var5 + "," + (XMLUtil.getIntAttr(var3, "optimization", 15) == 15 ? "Floating" : "Fixed");
      var5 = var5 + "\n";
      var5 = var5
         + OptimizationConst.printWFByPeriodType(
            var4, XMLUtil.getIntAttr(var3.getChild("Param2"), "value", 10), XMLUtil.getIntAttr(var3.getChild("Param1"), "value", 20)
         );
      var5 = var5 + "\n";
      var5 = var5 + L.t("Optimize Periods: %s", new Object[]{XMLUtil.getNodeBooleanValue(var2, "OptimizePeriods", false) ? "Yes" : "No"});
      var5 = var5 + "," + L.t("Optimize Exit Types: %s", new Object[]{XMLUtil.getNodeBooleanValue(var2, "OptimizeExitTypes", false) ? "Yes" : "No"});
      var5 = var5 + "," + L.t("Max Tests: %s", new Object[]{XMLUtil.getNodeIntValue(var2, "MaxTests", 100)});
      var5 = var5 + "\n\n";
      Element var6 = var1.getChild("AcceptanceSettings");
      var5 = var5 + L.t("ThresholdPct: %s", new Object[]{XMLUtil.getIntAttr(var6, "thresholdPct", 10)});
      var5 = var5 + "\n";
      Element var7 = var6.getChild("Conditions");
      ArrayList var8 = ProjectConfigHelper.getConditions(var7);

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Condition var10 = (Condition)var8.get(var9);
         if (var10.isUsed()) {
            var5 = var5 + var10.toString();
            var5 = var5 + "\n";
         }
      }

      return var5;
   }

   public int getBadStrategyReason() {
      return 10035;
   }

   public boolean disabledForSpecialTrial() {
      return true;
   }
}
