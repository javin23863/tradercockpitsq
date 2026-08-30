package com.strategyquant.plugin.CrossCheck.impl.OptProfileSysParamPermutation;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.WalkForwardCrossCheckMethod;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.OptProfileChecksLevels;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.SysParamPermutationsTable;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class OptProfileSysParamPermutation extends WalkForwardCrossCheckMethod implements IServletPlugin {
   private static final String CROSSCHECK_NAME = "OptProfileSysParamPermutation_CrossCheck";
   private ServletContextHandler dataContext;
   private OptProfileChecksLevels optProfileLevels;
   private int distributionUp = 30;
   private int distributionDown = 30;
   private int maxSteps = 10;
   private ValuesMap paramTypesOPSPP = new ValuesMap();

   public OptProfileSysParamPermutation() {
      super("OptProfileSysParamPermutation_CrossCheck");
      this.optProfileLevels = new OptProfileChecksLevels();
   }

   public String getName() {
      return NameOptProfileSPP;
   }

   public String getShortName() {
      return L.tsq("Opt. Profile / SPP");
   }

   public String getDescription() {
      return L.tsq("This cross check performs optimization of the strategy and then evaluates its Optimization profile - see the conditions in filter.");
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/optProfileSysParamPerm/");
         this.dataContext.addServlet(new ServletHolder(new OptProfileSysParamPermutationServlet()), "/*");
      }

      return this.dataContext;
   }

   public String getSettingName() {
      return this.getClass().getSimpleName();
   }

   public int getType() {
      return 2;
   }

   public int getPreferredPosition() {
      return 260;
   }

   public int getNumberOfSimulations() {
      return 0;
   }

   public boolean doesRetest() {
      return true;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public void fixSettings(Element var1) {
      super.fixSettings(var1);
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getParentElement().getParentElement();
      this.elRankings = var3.getChild("Rankings");
      Element var4 = var1.getChild("Settings");
      this.optimizationSettings.maxOptimizationBacktests = XMLUtil.getNodeIntValue(var4, "MaxTests", 100);
      this.distributionUp = XMLUtil.getNodeIntValue(var4, "DistributionUp", 100);
      this.distributionDown = XMLUtil.getNodeIntValue(var4, "DistributionDown", 100);
      this.maxSteps = XMLUtil.getNodeIntValue(var4, "Steps", 25);
      Element var5 = var4.getChild("WhatToParametrize");
      this.readWhatToParametrize(var5);
      this.paramTypesOPSPP = this.paramTypes.clone();
      Element var6 = var1.getChild("AcceptanceSettings");
      if (var6 == null) {
         var6 = XMLUtil.tryAddElement(var1, "AcceptanceSettings");
      }

      try {
         Element var7 = XMLUtil.tryAddElement(var6, "Conditions");
         this.settings.set("Conditions", ProjectConfigHelper.getConditions(var7));
         this.optimizationSettings.elConditions = var7;
         this.optimizationSettings.conditions = ProjectConfigHelper.getConditions(var7);
      } catch (Exception var8) {
         throw new Exception(L.t("Cannot read conditions of Crosscheck method '%s'", new Object[]{this.getName()}), var8);
      }

      this.optProfileLevels.evalProfitOptCheck = XMLUtil.getNodeBooleanValue(var6, "EvalProfitOptCheck", true);
      this.optProfileLevels.evalAvgProfitCheck = XMLUtil.getNodeBooleanValue(var6, "EvalAvgProfitCheck", true);
      this.optProfileLevels.evalUniformDistrCheck = XMLUtil.getNodeBooleanValue(var6, "EvalUniformDistrCheck", true);
      this.optProfileLevels.evalTopProfitCheck = XMLUtil.getNodeBooleanValue(var6, "EvalTopProfitCheck", true);
      this.optProfileLevels.profitableOptPct = XMLUtil.getNodeIntValue(var6, "ProfitOptPct", 30);
      this.optProfileLevels.avgProfit = XMLUtil.getNodeIntValue(var6, "AvgProfit", 0);
      this.optProfileLevels.uniformDistrChanges = XMLUtil.getNodeIntValue(var6, "UniformDistrChanges", 5);
      this.optProfileLevels.stdevAvgProfit = XMLUtil.getDouble(var6, "StdevAvgProfit", 1.0);
   }

   protected SettingsMap prepareSettings(String var1, SettingsMap var2, Element var3, boolean var4) throws Exception {
      try {
         SettingsMap var5 = this.cloneSettings(var2);
         ChartSetups var6 = new ChartSetups();
         var6.add((ChartSetup)var5.get("BacktestChart"));
         var5.set("ChartSetups", var6);
         var5.set("StrategyXml", var3);
         StrategyBase var7 = StrategyBase.createXmlStrategy(var3);
         var7.transformToVariables(this.paramSymmetry, this.paramTypesOPSPP);
         Variables var8 = var7.variables();
         var8.sortByName();
         var5.set("StrategyObject", var7);
         var5.set("StrategyName", var1);
         this.optimizationSettings.parameters = this.initializeParams(var7, this.distributionUp, this.distributionDown, this.maxSteps);
         this.optimizationSettings.optimizationMethod = 0;
         this.optimizationSettings.type = 4;
         this.optimizationSettings.singleThreaded = var4;
         var5.set("OptimizationSettings", this.optimizationSettings.clone());
         IFitnessFunction var9 = ProjectConfigHelper.getFitnessFunction(this.elRankings);
         var5.set("FitnessFunction", var9);
         return var5;
      } catch (Exception var10) {
         Log.error(SQUtils.getStackTrace(var10));
         throw new Exception(L.t("Error while preparing settings - %s", new Object[]{var10.getMessage()}));
      }
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      boolean var9 = super.runTest(var1, var2, var3, var5, var6, var7, var8);
      return !var9 ? false : this.checkOptimizationProfileConditions(var1);
   }

   protected void processResult(ResultsGroup var1, ResultsGroup var2) {
      try {
         OptimizationProfile var3 = var2.getOptimizationProfile();
         OptimizationProfile var4 = var1.getOptimizationProfile();
         if (var4 == null && var3 != null) {
            var1.setOptimizationProfile(var3);
         }

         var2.orders().clear();
      } catch (Exception var5) {
         Log.info("Error processing Optimization result", var5);
      }
   }

   private boolean checkOptimizationProfileConditions(ResultsGroup var1) throws Exception {
      var1.specialValues().set("OptProfileChecksLevels", this.optProfileLevels);
      OptimizationProfile var2 = var1.getOptimizationProfile();
      if (var2 == null) {
         return true;
      } else if (!var2.evaluateChecks(this.optProfileLevels)) {
         this.dismissalReason = var2.getDismissalReason();
         this.dismissalMessage = L.t("Cross Check filter in '%s': %s", new Object[]{this.getName(), var2.getDismissalMessage()});
         return false;
      } else {
         return true;
      }
   }

   protected boolean checkConditions(ResultsGroup var1, int var2) {
      ArrayList var3 = (ArrayList)this.settings.get("Conditions");
      if (var3 != null && var3.size() > 0) {
         for (int var4 = 0; var4 < var3.size(); var4++) {
            Condition var5 = (Condition)var3.get(var4);
            if (var5.isUsed()) {
               try {
                  boolean var6 = this.conditionsChecker.check(var1, var5);
                  if (!var6) {
                     this.dismissalReason = 300000 + var4 + var2 * 1000;
                     this.dismissalMessage = L.t("Cross Check filter in '%s': %s", new Object[]{this.getName(), this.conditionsChecker.dismissalMessage});
                     return false;
                  }
               } catch (CrossCheckDataNotExistException var7) {
               } catch (Exception var8) {
                  Log.error("Error while checking condition", var8);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = L.t("Cross Check filter in '%s': Error evaluating conditions: %s", new Object[]{this.getName(), var8.getMessage()});
                  return false;
               }
            }
         }
      }

      return true;
   }

   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      OptimizationProfile var5 = var1.getOptimizationProfile();
      if (var5 == null) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      SysParamPermutationsTable var6 = var5.getSysParamPermutationsTable();
      return var6.getMedian(var2);
   }

   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      OptimizationProfile var5 = var1.getOptimizationProfile();
      return var5 != null;
   }

   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      double var5 = this.getStatsValue(var1, var2, var3, var4);
      DatabankColumn var7 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var7.printPlValue(var5, (byte)10);
   }

   public String getColumnTitle(String var1, Element var2, Object... var3) {
      String var4 = this.getColumnTitleTemplate();
      return var4.replace("#columnName#", var1);
   }

   public String getColumnTitleTemplate() {
      return "#columnName# (Median)";
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      return null;
   }

   public ICrossCheck clone(SettingsMap var1) {
      OptProfileSysParamPermutation var2 = new OptProfileSysParamPermutation();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      var2.elRankings = this.elRankings;
      var2.optimizationSettings = this.optimizationSettings.clone();
      var2.optProfileLevels = this.optProfileLevels.clone();
      var2.distributionUp = this.distributionUp;
      var2.distributionDown = this.distributionDown;
      var2.maxSteps = this.maxSteps;
      var2.paramTypesOPSPP = this.paramTypes.clone();
      return var2;
   }

   public String printSettings(Element var1) throws Exception {
      if (var1 == null) {
         throw new NullPointerException();
      }

      if (!XMLUtil.elementIs(var1, "use")) {
         return "Not used";
      }

      Element var2 = var1.getChild("Settings");
      String var3 = "";
      var3 = L.t("Optimize Periods: %s", new Object[]{XMLUtil.getNodeBooleanValue(var2, "OptimPeriods", false) ? "Yes" : "No"});
      var3 = var3 + "," + L.t("Optimize Exit Types: %s", new Object[]{XMLUtil.getNodeBooleanValue(var2, "OptimExitTypes", false) ? "Yes" : "No"});
      var3 = var3 + "," + L.t("Max Tests: %s", new Object[]{XMLUtil.getNodeIntValue(var2, "MaxTests", 100)});
      var3 = var3 + "\n\n";
      Element var4 = var1.getChild("AcceptanceSettings");
      if (XMLUtil.getNodeBooleanValue(var4, "EvalProfitOptCheck", true)) {
         var3 = var3 + L.t("ProfitOptPct: %s", new Object[]{XMLUtil.getNodeIntValue(var4, "ProfitOptPct", 30)});
      }

      if (XMLUtil.getNodeBooleanValue(var4, "EvalAvgProfitCheck", true)) {
         var3 = var3 + "," + L.t("AvgProfit: %s", new Object[]{XMLUtil.getNodeIntValue(var4, "AvgProfit", 0)});
      }

      if (XMLUtil.getNodeBooleanValue(var4, "EvalUniformDistrCheck", true)) {
         var3 = var3 + "," + L.t("UniformDistrChanges: %s", new Object[]{XMLUtil.getNodeIntValue(var4, "UniformDistrChanges", 5)});
      }

      if (XMLUtil.getNodeBooleanValue(var4, "EvalTopProfitCheck", true)) {
         var3 = var3 + "," + L.t("StdevAvgProfit: %s", new Object[]{XMLUtil.getDouble(var4, "StdevAvgProfit", 1.0)});
      }

      var3 = var3 + "\n";
      Element var5 = var4.getChild("Conditions");
      ArrayList var6 = ProjectConfigHelper.getConditions(var5);

      for (int var7 = 0; var7 < var6.size(); var7++) {
         Condition var8 = (Condition)var6.get(var7);
         if (var8.isUsed()) {
            var3 = var3 + var8.toString();
            var3 = var3 + "\n";
         }
      }

      return var3;
   }

   public int getBadStrategyReason() {
      return 10032;
   }
}
