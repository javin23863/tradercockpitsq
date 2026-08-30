package com.strategyquant.plugin.CrossCheck.impl.RetestWithHigherPrecision;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class RetestWithHigherPrecision extends CrossCheckMethod implements IFitnessFunction, IServletPlugin {
   private ServletContextHandler dataContext;
   private String databankColumnName = null;
   private ArrayList<RetestWithHigherPrecision.Goal> weightedGoals = new ArrayList<>();

   public ICrossCheck clone(SettingsMap var1) {
      RetestWithHigherPrecision var2 = new RetestWithHigherPrecision();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      return var2;
   }

   public String getName() {
      return NameHigherBacktestPrecis;
   }

   public String getShortName() {
      return L.t("Higher backtest precis.", new Object[0]);
   }

   public String getDescription() {
      return L.tsq(
         "This cross checks retests the strategy with higher test precision.<br/>It can be used if you are using the lowest 'Selected timeframe' precision in the main backtest to quickly find and evaluate the valid strategies. You can then use this cross check to retest the strategy with higher precision to see if it performs in a same way with higher precision backtesting."
      );
   }

   public String getSettingName() {
      return "RetestWithHigherPrecision";
   }

   public int getType() {
      return 0;
   }

   public int getPreferredPosition() {
      return 300;
   }

   public int getNumberOfSimulations() {
      return 1;
   }

   public boolean doesRetest() {
      return true;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getChild("Settings");
      Element var4 = var3.getChild("Precision");
      Element var5 = var3.getChild("Spread");

      try {
         this.settings.set("BacktestPrecision", Integer.parseInt(var4.getValue()));
      } catch (Exception var7) {
         Log.warn("Cannot load backtest precision setting. Using default...", var7);
         this.settings.set("BacktestPrecision", 1);
      }

      try {
         if (this.settings != null && var5 != null) {
            this.settings.set("BacktestSpread", Double.parseDouble(var5.getValue()));
         } else {
            this.settings.set("BacktestSpread", 1);
         }
      } catch (Exception var8) {
         Log.warn("Cannot load backtest spread setting. Using default...", var8);
         this.settings.set("BacktestSpread", 1);
      }

      this.readConditions(var1);
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      int var9 = (Integer)this.settings.get("BacktestPrecision");
      double var10 = this.settings.getDouble("BacktestSpread", -2.0);
      OrdersList var12 = var1.orders();
      String var13 = var1.getMainResultKey();
      Result var14 = var1.subResult(var13);
      SettingsMap var15 = var14.getSettings();
      SettingsMap var16 = this.cloneSettings(var15);
      ChartSetup var17 = (ChartSetup)var16.get("BacktestChart");
      var17.setTestPrecision(var9);
      if (var7 != null) {
         String var18 = L.t("Running higher precision backtest for %s", new Object[]{var8});
         var7.setLastEvent(var18);
      }

      if (var10 != -2.0) {
         ArrayList var24 = var17.getCharts();
         int var19 = 0;

         for (int var20 = 0; var20 < var24.size(); var20++) {
            ChartDef var21 = (ChartDef)var24.get(var20);
            if (var20 == 0 || var21.getSymbolHash() == var19) {
               var19 = var17.getMainChart().getSymbolHash();
               var21.setSpread(var10);
            }
         }
      }

      BacktestEngine var25 = new BacktestEngine(TraderSimulators.getSimulator(var17));
      var25.setSingleThreaded(true);
      var25.setUsePreparedData(true);
      var25.setStopPauseEngine(this.stopPauseEngine);
      var25.registerProgressListener(new IBacktestProgressListener() {
         public void setProgress(int var1) {
            RetestWithHigherPrecision.this.setJobProgress(var1);
         }

         public void increaseProgressStep() {
         }
      });
      var25.addSetup(var16);
      ResultsGroup var26 = var25.runBacktest().getResults();
      String var27 = "CrossCheck_HigherPrecision";
      OrdersList var28 = var26.orders();

      for (int var22 = 0; var22 < var28.size(); var22++) {
         Order var23 = var28.get(var22);
         var23.SetupName = var27;
         var23.IsInPortfolio = 0;
      }

      var1.addSubresult(var27, var15);
      Result var29 = var1.subResult(var27);
      var29.setSpecial(true);
      var29.setFrom(var26.subResult(var13), var27);
      var12.addAll(var28);
      if (this.backtestProgressListener != null) {
         this.backtestProgressListener.increaseProgressStep();
      }

      return this.checkConditions(var1, var2);
   }

   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (!var1.getResultKeys().contains("CrossCheck_HigherPrecision")) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      byte var5 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var6 = this.getSampleType(var3, var4);
      byte var7 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getNumericValue(var1, "CrossCheck_HigherPrecision", var5, var7, var6);
   }

   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      return var1.getResultKeys().contains("CrossCheck_HigherPrecision");
   }

   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (!var1.getResultKeys().contains("CrossCheck_HigherPrecision")) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      byte var5 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var6 = this.getSampleType(var3, var4);
      byte var7 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getValue(var1, var1.getMainResultKey(), var5, var7, var6);
   }

   public String getColumnTitle(String var1, Element var2, Object... var3) {
      byte var4 = XMLUtil.getByteAttr(var2, "direction", (byte)0);
      byte var5 = this.getSampleType(var2, var3);
      byte var6 = XMLUtil.getByteAttr(var2, "plType", (byte)10);
      String var7 = this.getColumnTitleTemplate();
      var7 = var7.replace("#columnName#", var1);
      var7 = var7.replace(", #sampleType#", var5 == 127 ? "" : ", " + SampleTypes.typeToString(var5));
      var7 = var7.replace(", #direction#", var4 == 0 ? "" : ", " + Directions.typeToString(var4));
      return var7.replace(", #plType#", var6 == 10 ? "" : ", " + PlTypes.typeToString(var6));
   }

   public String getColumnTitleTemplate() {
      return "#columnName# (" + this.getShortName() + ", #sampleType#, #direction#, #plType#)";
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      ChartSetup var2 = var1.getClone();
      int var3 = (Integer)this.settings.get("BacktestPrecision");
      var2.setTestPrecision(var3);
      return new ChartSetups(var2);
   }

   public boolean doesCreateSubjobs() {
      return false;
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
      int var4 = Integer.parseInt(var2.getChild("Precision").getValue());
      var3 = L.t("Backtest precision: %s", new Object[]{Precisions.toString(var4)});
      var3 = var3 + "," + L.t("Spread: %s", new Object[]{var2.getChildText("Spread")});
      var3 = var3 + "\n";
      Element var5 = var1.getChild("AcceptanceSettings").getChild("Conditions");
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
      return 10036;
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/retestWithHigherPrecision/");
         this.dataContext.addServlet(new ServletHolder(new RetestWithHigherPrecisionServlet()), "/*");
      }

      return this.dataContext;
   }

   public double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      return this.computeFitness(var1, var2, var3, true);
   }

   public double computeFitness(ResultsGroup var1, byte var2, byte var3, boolean var4) throws Exception {
      return this.databankColumnName.equals("Weighted")
         ? this.computeWeightedFitness(var1, var2, var3)
         : this.getFitnessValue(var1, var2, var3, this.databankColumnName, 0.0, (byte)0);
   }

   private double getFitnessValue(ResultsGroup var1, byte var2, byte var3, String var4, double var5, byte var7) throws Exception {
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var4);
      double var9;
      if (!var1.hasResult("CrossCheck_HigherPrecision")) {
         var9 = var8.getNumericValue(var1, "Portfolio", var2, (byte)10, var3);
      } else {
         var9 = var8.getNumericValue(var1, "CrossCheck_HigherPrecision", var2, (byte)10, var3);
      }

      double var11 = var8.transformToFitnessRange(var9, var5, var7);
      if (var11 < 0.0) {
         var11 = 0.0;
      }

      if (var11 > 1.0) {
         var11 = 1.0;
      }

      return var11;
   }

   private double computeWeightedFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      float var4 = 0.0F;

      for (int var5 = 0; var5 < this.weightedGoals.size(); var5++) {
         if (this.weightedGoals.get(var5).use) {
            var4 = (float)(var4 + this.weightedGoals.get(var5).weight);
         }
      }

      float var10 = 0.0F;

      for (int var6 = 0; var6 < this.weightedGoals.size(); var6++) {
         RetestWithHigherPrecision.Goal var7 = this.weightedGoals.get(var6);
         if (var7.use) {
            double var8 = this.getFitnessValue(var1, var2, var3, var7.statsValueName, var7.target, var7.valueType);
            var10 = (float)(var10 + var7.weight / var4 * var8);
         }
      }

      return var10;
   }

   public String getFitnessKey() {
      return this.getSettingName();
   }

   public String getFitnessName() {
      return "Cross check - Higher backtest precision";
   }

   public void initFitnessFromXml(Element var1) {
      this.weightedGoals.clear();
      Element var2 = var1.getChild("Ranking");
      this.databankColumnName = var2.getAttributeValue("type");

      for (Element var4 : var2.getChildren("Goal")) {
         RetestWithHigherPrecision.Goal var5 = new RetestWithHigherPrecision.Goal();
         String var6 = var4.getAttributeValue("use");
         var5.use = var6.equals("true") || var6.equalsIgnoreCase("1");
         var5.statsValueName = var4.getAttributeValue("type");
         var5.weight = XMLUtil.getDoubleAttr(var4, "weight", 1.0);
         var5.valueType = XMLUtil.getByteAttr(var4, "valueType", (byte)0);
         var5.target = XMLUtil.getDoubleAttr(var4, "target", 0.0);
         this.weightedGoals.add(var5);
      }
   }

   public byte getFitnessType() throws Exception {
      return this.databankColumnName.equals("Weighted") ? 1 : ((DatabankColumn)DatabankColumns.get().findClassByName(this.databankColumnName)).valueType;
   }

   public String getFitnessDatabankColumnName() {
      return this.databankColumnName;
   }

   public ArrayList<DatabankColumn> getUsedStatValues() throws Exception {
      throw new Exception("This fitness method cannot be used in Walk-Forward!");
   }

   public String printWeightedGoals() throws Exception {
      return null;
   }

   public ArrayList<MetricForFitness> getMetricsForFitness() {
      return null;
   }

   public double getMetricValue(ResultsGroup var1, byte var2, byte var3, String var4) throws Exception {
      return 0.0;
   }

   public IFitnessFunction clone() {
      return this;
   }

   public String forEngine() {
      return "*,-SP,-SA";
   }

   private class Goal {
      public boolean use;
      public String statsValueName;
      public double weight;
      public double target;
      public byte valueType;

      private Goal() {
      }
   }
}
