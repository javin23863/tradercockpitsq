package com.strategyquant.tradinglib.crosscheck;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CrossCheckMethod implements ICrossCheck {
   public static final String RetestOnAdditionalMarkets = "RetestOnAdditionalMarkets";
   public static final String OptProfileSysParamPermutation = "OptProfileSysParamPermutation";
   public static final String WhatIf = "WhatIf";
   public static final String RetestWithHigherPrecision = "RetestWithHigherPrecision";
   public static final String NameMCManipulation = L.tsq("Monte Carlo trades manipulation");
   public static final String NameMCRetest = L.tsq("Monte Carlo retest methods");
   public static final String NameOptProfileSPP = L.tsq("Opt. Profile / Sys. Param. Permutation");
   public static final String NameAddMarkets = L.tsq("Backtests on additional markets");
   public static final String NameWFMatrix = L.tsq("Walk-Forward Matrix");
   public static final String NameWFOptimization = L.tsq("Walk-Forward Optimization");
   public static final String NameHigherBacktestPrecis = L.tsq("Higher backtest precision");
   public static final String NameWhatIf = L.tsq("What If simulations");
   public static final String NameSequentialOptimization = L.tsq("Sequential Optimization");
   public static final Logger Log = LoggerFactory.getLogger("CrossCheckMethod");
   protected SettingsMap settings = new SettingsMap();
   public static int[] ConfidenceLevels = new int[]{50, 60, 70, 80, 90, 92, 95, 97, 98, 99, 100};
   public static double[] InitialCapitalVariants = new double[]{0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0, 6.0};
   protected static final StatsTypeCombination combination = new StatsTypeCombination((byte)0, (byte)10, (byte)127);
   protected int dismissalReason = 0;
   protected String dismissalMessage = "";
   protected IBacktestProgressListener backtestProgressListener;
   protected StopPauseEngine stopPauseEngine;
   protected ConditionsChecker conditionsChecker = new ConditionsChecker();

   public String getProduct() {
      return "SQUANTBACKTESTNODE";
   }

   public abstract int getPreferredPosition();

   public void initPlugin() throws Exception {
   }

   @Override
   public String getDescription() {
      return null;
   }

   @Override
   public SettingsMap getSettings() {
      return this.settings;
   }

   @Override
   public void fixSettings(Element var1) {
      XMLUtil.tryAddElement(var1, "Settings");
      Element var2 = XMLUtil.tryAddElement(var1, "AcceptanceSettings");
      XMLUtil.tryAddElement(var2, "Conditions");
      if (!MainApp.v571hfnsHw().gDmtOfRJBr()) {
         var1.setAttribute("use", "false");
      }
   }

   @Override
   public int getDismissalReason() {
      return this.dismissalReason;
   }

   @Override
   public String getDismissalMessage() {
      return this.dismissalMessage;
   }

   protected void readConditions(Element var1) throws Exception {
      Element var2 = XMLUtil.tryAddElement(var1, "AcceptanceSettings");

      try {
         Element var3 = XMLUtil.tryAddElement(var2, "Conditions");
         this.settings.set("Conditions", ProjectConfigHelper.getConditions(var3));
      } catch (Exception var4) {
         throw new Exception(L.t("Cannot read conditions of Crosscheck method '%s'", new Object[]{this.getName()}), var4);
      }
   }

   private String getPluginName() {
      return this.getClass().getSimpleName();
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
                     this.dismissalMessage = String.format("Cross Check filter in '%s': %s", this.getName(), this.conditionsChecker.dismissalMessage);
                     return false;
                  }
               } catch (CrossCheckDataNotExistException var7) {
               } catch (Exception var8) {
                  Log.error("Error while checking condition", var8);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = String.format("Cross Check filter in '%s': Error evaluating conditions: %s", this.getName(), var8.getMessage());
                  return false;
               }
            }
         }
      }

      return true;
   }

   protected SettingsMap cloneSettings(SettingsMap var1) throws CloneNotSupportedException {
      SettingsMap var2 = new SettingsMap();
      String[] var3 = var1.getAllKeys();

      for (int var4 = 0; var4 < var3.length; var4++) {
         Object var5 = this.cloneValue(var3[var4], var1.get(var3[var4]));
         var2.set(var3[var4], var5);
      }

      var2.set("StrategyCheckDate", -1L);
      return var2;
   }

   private Object cloneValue(String var1, Object var2) throws CloneNotSupportedException {
      if (var1.equals("TradingOptions")) {
         return ((TradingOptions)var2).getClone();
      } else if (var1.equals("StoreChartData")) {
         return false;
      } else if (var2 instanceof StrategyBase) {
         return ((StrategyBase)var2).clone();
      } else {
         return var1.equals("BacktestChart") ? ((ChartSetup)var2).getClone() : var2;
      }
   }

   protected void setJobProgress(int var1) {
      if (this.backtestProgressListener != null) {
         this.backtestProgressListener.setProgress(var1);
      }
   }

   @Override
   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }

   @Override
   public void setStopPauseEngine(StopPauseEngine var1) {
      this.stopPauseEngine = var1;
   }

   @Override
   public void saveDataToFile(ResultsGroup var1, JarOutputStream var2) throws Exception {
   }

   @Override
   public void loadDataFromFile(ResultsGroup var1, JarFile var2) throws Exception {
   }

   protected byte getSampleType(Element var1, Object... var2) {
      byte var3 = XMLUtil.getByteAttr(var1, "sampleType", (byte)127);
      if ((var3 == 66 || var3 == 77) && var2.length > 1 && var2[1] instanceof HashMap) {
         HashMap var4 = (HashMap)var2[1];
         if (var4.containsKey("SampleType")) {
            var3 = (Byte)var4.get("SampleType");
         }
      }

      return var3;
   }

   public boolean disabledForSpecialTrial() {
      return false;
   }

   protected void checkSpecialTrialLimitation() throws Exception {
      if (MainApp.v571hfnsHw().bDm88fRJA3()) {
         throw new Exception(L.t("Crosscheck '%s' is available only in the full version.", new Object[]{this.getName()}));
      }
   }
}
