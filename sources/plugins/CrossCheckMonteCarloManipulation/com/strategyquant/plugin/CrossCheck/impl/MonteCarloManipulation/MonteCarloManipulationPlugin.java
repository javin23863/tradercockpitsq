package com.strategyquant.plugin.CrossCheck.impl.MonteCarloManipulation;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.crosscheck.MCResultsComputer;
import com.strategyquant.tradinglib.crosscheck.MonteCarloCrossCheckMethod;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.SymbolsMap;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class MonteCarloManipulationPlugin extends MonteCarloCrossCheckMethod implements IServletPlugin {
   private ServletContextHandler dataContext;
   private final int[] originalStatsKeys = new int[]{"TotalTradingDays".hashCode(), "TotalTradingMonths".hashCode(), "TotalTradingYears".hashCode()};
   private int numberOfSimulations;
   private boolean useFullSample;

   public ICrossCheck clone(SettingsMap var1) {
      MonteCarloManipulationPlugin var2 = new MonteCarloManipulationPlugin();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      var2.numberOfSimulations = this.numberOfSimulations;
      var2.useFullSample = this.useFullSample;
      return var2;
   }

   public String getName() {
      return NameMCManipulation;
   }

   public String getShortName() {
      return L.tsq("MC trades");
   }

   public String getDescription() {
      return L.tsq(
         "This cross check uses Monte Carlo method to perform various simulations of resulting equity curve by manipulating the order of the trades.<br/>It allows you to quickly assess how much the good results are dependent on order of the trades."
      );
   }

   public String getSettingName() {
      return "MonteCarloManipulation";
   }

   public int getType() {
      return 0;
   }

   public int getPreferredPosition() {
      return 230;
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/monteCarloManipulation/");
         this.dataContext.addServlet(new ServletHolder(new MonteCarloManipulationServlet()), "/*");
      }

      return this.dataContext;
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
      this.settings.set("MonteCarloManipulation", ProjectConfigHelper.getMonteCarloManipulationMethods(var3.getChild("Methods")));
      this.readConditions(var1);
      this.numberOfSimulations = (Integer)this.settings.get("RTNumberOfSimulations");
      this.useFullSample = (Boolean)this.settings.get("MCUseFullSample");
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      ArrayList var9 = (ArrayList)this.settings.get("MonteCarloManipulation");
      this.numberOfSimulations = (Integer)this.settings.get("RTNumberOfSimulations");
      this.useFullSample = (Boolean)this.settings.get("MCUseFullSample");
      String var10 = var1.getMainResultKey();
      SymbolsMap var11 = var1.symbols();
      MersenneTwisterRng var12 = new MersenneTwisterRng();
      Result var14 = var1.subResult(var10);
      ArrayList var15 = new ArrayList();
      SettingsMap var16 = var14.getSettings();
      OrdersList var17 = var1.orders();
      OrdersList var18 = var17.filterWithClone(var10, (byte)0, (byte)(this.useFullSample ? 127 : 10));
      this.getOrCreateOriginalOrders(var14, var18, var10, var16, var11);
      OrdersList var19 = new OrdersList("MC orders simulation");

      for (int var20 = 0; var20 < this.numberOfSimulations; var20++) {
         this.initializeSimulatedOrders(var19, var18);

         for (int var21 = 0; var21 < var9.size(); var21++) {
            ((MonteCarloManipulation)var9.get(var21)).modifyTrades(var12, var19);
         }

         RobustnessResults var13 = MCResultsComputer.computeResults(var19, var16, var11, var14);
         if (var13.getOrdersValues().size() < 2) {
            Log.debug("MC manipulation job returned empty result");
         } else {
            var15.add(var13);
            var14.addMCSimulation(this.getSettingName(), var13);
            if (this.backtestProgressListener != null) {
               this.backtestProgressListener.increaseProgressStep();
            }
         }
      }

      var16.removeIgnoredKeys(this.originalStatsKeys);
      var14.set(this.getSettingName() + "_NumberOfSimulations", this.numberOfSimulations);
      String[] var25 = new String[var9.size()];

      for (int var26 = 0; var26 < var9.size(); var26++) {
         var25[var26] = ((MonteCarloManipulation)var9.get(var26)).printFormatedName();
      }

      var14.set(this.getSettingName() + "_Methods", var25);
      String var27 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Symbol);
      String var22 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Timeframe);
      String var23 = SQTime.toUIDateString(var1.specialValues().getLong(SpecialValues.HistoryFrom));
      String var24 = SQTime.toUIDateString(var1.specialValues().getLong(SpecialValues.HistoryTo));
      var14.set(this.getSettingName() + "_Symbol", var27);
      var14.set(this.getSettingName() + "_TimeFrame", var22);
      var14.set(this.getSettingName() + "_DateRange", var23 + " - " + var24);
      this.computeConfidenceLevels(var14, var15);
      return this.checkConditions(var1, var2);
   }

   private void initializeSimulatedOrders(OrdersList var1, OrdersList var2) {
      int var3 = Math.min(var1.size(), var2.size());

      for (int var4 = 0; var4 < var3; var4++) {
         var1.get(var4).setFromOrder(var2.get(var4));
      }

      if (var1.size() < var2.size()) {
         for (int var6 = var3; var6 < var2.size(); var6++) {
            var1.add(new Order(var2.get(var6)));
         }
      }

      int var7 = var1.size() - var2.size();
      if (var7 > 0) {
         for (int var5 = 0; var5 < var7; var5++) {
            var1.remove(var1.size() - 1);
         }
      }
   }

   public int getNumberOfSimulations() {
      return this.numberOfSimulations;
   }

   public boolean doesRetest() {
      return false;
   }

   public boolean doesForEverySetup() {
      return false;
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
      var3 = var3 + "\n";
      ArrayList var4 = ProjectConfigHelper.getMonteCarloManipulationMethods(var2.getChild("Methods"));

      for (int var5 = 0; var5 < var4.size(); var5++) {
         MonteCarloManipulation var6 = (MonteCarloManipulation)var4.get(var5);
         var3 = var3 + var6.printFormatedName();
         var3 = var3 + "\n";
      }

      var3 = var3 + "\n";
      Element var15 = var1.getChild("AcceptanceSettings").getChild("Conditions");
      ArrayList var16 = ProjectConfigHelper.getConditions(var15);

      for (int var7 = 0; var7 < var16.size(); var7++) {
         Condition var8 = (Condition)var16.get(var7);
         if (var8.isUsed()) {
            var3 = var3 + var8.toString();
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
      return 10030;
   }
}
