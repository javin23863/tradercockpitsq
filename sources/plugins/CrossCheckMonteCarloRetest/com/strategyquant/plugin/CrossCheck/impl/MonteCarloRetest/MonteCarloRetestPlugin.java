package com.strategyquant.plugin.CrossCheck.impl.MonteCarloRetest;

import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.MonteCarloCrossCheckMethod;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class MonteCarloRetestPlugin extends MonteCarloCrossCheckMethod implements IServletPlugin {
   private ServletContextHandler dataContext;
   private int numberOfSimulations;
   private int lastIndex;
   private boolean useFullSample;
   private int backtestPrecision;

   public ICrossCheck clone(SettingsMap var1) {
      MonteCarloRetestPlugin var2 = new MonteCarloRetestPlugin();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      var2.numberOfSimulations = this.numberOfSimulations;
      var2.useFullSample = this.useFullSample;
      var2.backtestPrecision = this.backtestPrecision;
      return var2;
   }

   public String getName() {
      return NameMCRetest;
   }

   public String getShortName() {
      return L.tsq("MC retest");
   }

   public String getDescription() {
      return L.tsq(
         "This cross check uses Monte Carlo methods that perform simulations that require retesting the strategy - using different spread, parameters or history data.<br/>Note that it is slow, strategy will have to be retested multiple times, depending on your number of simulations."
      );
   }

   public String getSettingName() {
      return "MonteCarloRetest";
   }

   public int getType() {
      return 1;
   }

   public int getPreferredPosition() {
      return 240;
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/monteCarloRetest/");
         this.dataContext.addServlet(new ServletHolder(new MonteCarloRetestServlet()), "/*");
      }

      return this.dataContext;
   }

   public int getNumberOfSimulations() {
      return this.numberOfSimulations;
   }

   public boolean doesRetest() {
      return true;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public void fixSettings(Element var1) {
      super.fixSettings(var1);
      Element var2 = var1.getChild("Settings");
      XMLUtil.tryAddElement(var2, "Methods");
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getChild("Settings");
      this.settings.set("MCUseFullSample", XMLUtil.getNodeBooleanValue(var3, "MCUseFullSample", false));
      this.settings.set("RTNumberOfSimulations", XMLUtil.getNodeIntValue(var3, "NumberOfSimulations", 10));
      this.settings.set("MonteCarloManipulation", ProjectConfigHelper.getMonteCarloRetestMethods(var3.getChild("Methods")));
      this.settings.set("MCBacktestPrecision", XMLUtil.getNodeIntValue(var3, "MCBacktestPrecision", -1));
      this.readConditions(var1);
      this.numberOfSimulations = (Integer)this.settings.get("RTNumberOfSimulations");
      this.useFullSample = (Boolean)this.settings.get("MCUseFullSample");
      this.backtestPrecision = (Integer)this.settings.get("MCBacktestPrecision");
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      this.numberOfSimulations = (Integer)this.settings.get("RTNumberOfSimulations");
      this.useFullSample = (Boolean)this.settings.get("MCUseFullSample");
      this.backtestPrecision = (Integer)this.settings.get("MCBacktestPrecision");
      String var9 = var1.getMainResultKey();
      SymbolsMap var10 = var1.symbols();
      Result var12 = null;
      OrdersList var13 = var1.orders();
      var12 = var1.subResult(var9);
      SettingsMap var14 = var12.getSettings();
      int var15 = var14.getInt("BacktestPrecision", 1);
      OrdersList var16 = var13.filterWithClone(var9, (byte)0, (byte)(this.useFullSample ? 127 : 10));
      this.getOrCreateOriginalOrders(var12, var16, var9, var14, var10);
      ArrayList var11 = this.runOnGrid(var14, var15, var12, var10, var3, var8, var5, var7, var6, this.useFullSample, this.backtestPrecision);
      var12.set(this.getSettingName() + "_NumberOfSimulations", this.numberOfSimulations);
      ArrayList var17 = (ArrayList)this.settings.get("MonteCarloManipulation");
      String[] var18 = new String[var17.size()];

      for (int var19 = 0; var19 < var17.size(); var19++) {
         var18[var19] = ((MonteCarloRetest)var17.get(var19)).printFormatedName();
      }

      var12.set(this.getSettingName() + "_Methods", var18);
      String var24 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Symbol);
      String var20 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Timeframe);
      String var21 = SQTime.toUIDateString(var1.specialValues().getLong(SpecialValues.HistoryFrom));
      String var22 = SQTime.toUIDateString(var1.specialValues().getLong(SpecialValues.HistoryTo));
      var12.set(this.getSettingName() + "_Symbol", var24);
      var12.set(this.getSettingName() + "_TimeFrame", var20);
      var12.set(this.getSettingName() + "_DateRange", var21 + " - " + var22);
      this.computeConfidenceLevels(var12, var11);
      return this.checkConditions(var1, var2);
   }

   private ArrayList<RobustnessResults> runOnGrid(
      final SettingsMap var1,
      final int var2,
      final Result var3,
      final SymbolsMap var4,
      final double var5,
      final String var7,
      GridJob var8,
      final ILastEventListener var9,
      boolean var10,
      final boolean var11,
      final int var12
   ) throws Exception {
      final ArrayList var13 = new ArrayList();
      this.lastIndex = 0;
      SimpleGridEngine var14 = new SimpleGridEngine<MCRetestJob, MCJobResult>(this.stopPauseEngine, var8, var10) {
         protected ArrayList<MCRetestJob> createJobsBatch(int var1x, GridClient var2x) throws Exception {
            return MonteCarloRetestPlugin.this.createBatch(
               var7,
               var1x,
               var2x,
               MonteCarloRetestPlugin.this.numberOfSimulations,
               var1,
               MonteCarloRetestPlugin.this.settings,
               var9,
               var2,
               var5,
               var4,
               var11,
               var12
            );
         }

         protected void processResult(MCJobResult var1x, JobDetails var2x) throws Exception {
            MonteCarloRetestPlugin.this.processMCResult(var1x, var2x, var13, var3);
         }

         protected void onError(String var1x, Exception var2x) {
            Log.error("MC Error {}, Exception ", var1x, var2x);
         }
      };
      var14.start();
      var14.unregisterListeners();
      return var13;
   }

   protected void processMCResult(MCJobResult var1, JobDetails var2, ArrayList<RobustnessResults> var3, Result var4) {
      String var5 = var2.getJobID();
      String var6 = var1.getDismissalMessage();
      if (var6 != null) {
         Log.error(String.format("MC retest job %s dismissed, error: %s", var5, var6));
      } else {
         if (var2.isSuccess()) {
            RobustnessResults var7 = var1.getRR();
            if (var7 != null && var7.getOrdersValues().size() > 1) {
               var3.add(var7);
               var4.addMCSimulation(this.getSettingName(), var7);
            } else {
               Log.debug("MC retest job returned empty result");
            }
         } else {
            var2.getException();
            Log.error(String.format("Optimization %s failed, error: %s", var5, var2.getException()));
         }
      }
   }

   protected ArrayList<MCRetestJob> createBatch(
      String var1,
      int var2,
      GridClient var3,
      long var4,
      SettingsMap var6,
      SettingsMap var7,
      ILastEventListener var8,
      int var9,
      double var10,
      SymbolsMap var12,
      boolean var13,
      int var14
   ) throws Exception {
      ArrayList var15 = new ArrayList();
      int var16 = 0;

      for (long var17 = this.lastIndex; var17 < this.lastIndex + var2 && var17 < var4; var17++) {
         String var19 = String.format("%s MC %d", var1, var17);
         MCRetestJob var20 = new MCRetestJob(var17, var1, var19, getJobSettings(var6, var7, var13, var14), this.stopPauseEngine, var8, var9, var10, var12);
         var15.add(var20);
         var16++;
      }

      this.lastIndex += var16;
      return var15.size() == 0 ? null : var15;
   }

   private static Map<String, Serializable> getJobSettings(SettingsMap var0, SettingsMap var1, boolean var2, int var3) {
      HashMap var4 = new HashMap();
      var4.put("resultSettings", var0);
      var4.put("mcSettings", var1);
      var4.put("useFullSample", var2);
      var4.put("backtestPrecision", var3);
      return var4;
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
      String var3 = "";
      var3 = L.t("Number of simulations: %s", new Object[]{XMLUtil.getNodeIntValue(var2, "NumberOfSimulations", 10)});
      var3 = var3 + "," + L.t("Use Full sample: %s", new Object[]{XMLUtil.getNodeBooleanValue(var2, "MCUseFullSample", false)});
      int var4 = XMLUtil.getNodeIntValue(var2, "MCBacktestPrecision", -1);
      if (var4 == -1) {
         var3 = var3 + "," + L.t("Backtest precision: %s", new Object[]{"Same as main chart"});
      } else {
         var3 = var3 + "," + L.t("Backtest precision: %s", new Object[]{Precisions.toString(var4)});
      }

      var3 = var3 + "\n";
      ArrayList var5 = ProjectConfigHelper.getMonteCarloRetestMethods(var2.getChild("Methods"));

      for (int var6 = 0; var6 < var5.size(); var6++) {
         MonteCarloRetest var7 = (MonteCarloRetest)var5.get(var6);
         var3 = var3 + var7.printFormatedName();
         var3 = var3 + "\n";
      }

      var3 = var3 + "\n";
      Element var17 = var1.getChild("AcceptanceSettings").getChild("Conditions");
      ArrayList var18 = ProjectConfigHelper.getConditions(var17);

      for (int var8 = 0; var8 < var18.size(); var8++) {
         Condition var9 = (Condition)var18.get(var8);
         if (var9.isUsed()) {
            var3 = var3 + var9.toString();
            var3 = var3 + "\n";
         }
      }

      return var3;
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

   public int getBadStrategyReason() {
      return 10031;
   }

   public boolean doesCreateSubjobs() {
      return true;
   }
}
