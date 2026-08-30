package com.strategyquant.plugin.CrossCheck.impl.RetestOnAdditionalMarkets;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.MetricForFitness;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.simulator.impl.TraderSimulators;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class RetestOnAdditionalMarkets extends CrossCheckMethod implements IFitnessFunction, IServletPlugin {
   private ServletContextHandler dataContext;
   private static final String SETTING_SLIPPAGES = "CS_ROAM_SLIPPAGES";
   private static final String SETTING_MIN_DISTANCES = "CS_ROAM_MIN_DISTANCES";
   private static final String SETTING_COMMISSIONS = "CS_ROAM_COMMISSIONS";
   private static final String SETTING_SWAPS = "CS_ROAM_SWAPS";
   private static final String SETTING_USE_MAIN_TEST_DATE = "CS_ROAM_USE_MAINTETS_DATE";
   private static final String ConditionsCheckAdditionalMarkets = "ConditionsCheckMarkets";
   private static final String ConditionsMinConds = "ConditionsMinConds";
   private static final String ConditionsMinMarkets = "ConditionsMinMarkets";
   private String databankColumnName = null;
   private ArrayList<RetestOnAdditionalMarkets.Goal> weightedGoals = new ArrayList<>();

   public ICrossCheck clone(SettingsMap var1) {
      RetestOnAdditionalMarkets var2 = new RetestOnAdditionalMarkets();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      return var2;
   }

   public String getName() {
      return NameAddMarkets;
   }

   public String getShortName() {
      return L.t("Add. markets", new Object[0]);
   }

   public String getDescription() {
      return L.tsq(
         "This cross check retest the strategy on multiple additional markets (symbols).<br/>It allows you to compare the performance on multiple symbols, ideally you want to find the strategy that will work on multiple markets."
      );
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/retestOnAdditionalMarkets/");
         this.dataContext.addServlet(new ServletHolder(new RetestOnAdditionalMarketsServlet()), "/*");
      }

      return this.dataContext;
   }

   public String getSettingName() {
      return "RetestOnAdditionalMarkets";
   }

   public int getType() {
      return 1;
   }

   public int getPreferredPosition() {
      return 210;
   }

   public int getNumberOfSimulations() {
      ChartSetups var1 = (ChartSetups)this.settings.get("AdditionalChartSetups");
      return var1 == null ? 0 : var1.size();
   }

   public boolean doesRetest() {
      return true;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public void fixSettings(Element var1) {
      super.fixSettings(var1);
      String var2 = null;

      try {
         Element var3 = var1.getParentElement().getParentElement();
         Element var4 = var3.getChild("Data");
         if (var4 == null) {
            var4 = var3.getChild("CustomData");
         }

         Element var5 = var4.getChild("Setups").getChild("Setup").getChild("Chart");
         var2 = var5.getAttributeValue("timeframe");
      } catch (Exception var10) {
         Log.error("Incorrect Main data. Cannot get main data timeframe.", var10);
      }

      Element var11 = var1.getChild("Settings");

      for (Element var13 : var11.getChild("Setups").getChildren("Setup")) {
         List var6 = var13.getChildren("Chart");

         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var8 = (Element)var6.get(var7);
            String var9 = var8.getAttributeValue("timeframe");
            if (var9 != null && var2 != null && var9.equals("Same as main data")) {
               var8.setAttribute("timeframe", var2);
            }
         }
      }
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      try {
         Element var3 = var1.getChild("Settings");
         List var4 = var3.getChild("Setups").getChildren("Setup");
         if (var4.size() == 0) {
            throw new Exception();
         }
      } catch (Exception var6) {
         throw new Exception(L.t("No additional setups defined", new Object[0]));
      }

      try {
         this.fixSettings(var1);
      } catch (Exception var5) {
         Log.error("Cannot fix settings", var5);
         throw new Exception(L.t("Invalid additional setups settings", new Object[0]));
      }

      Element var7 = var1.getChild("Settings");
      this.settings.set("AdditionalChartSetups", ProjectConfigHelper.getChartSetups(var7));
      this.settings.set("CS_ROAM_SLIPPAGES", ProjectConfigHelper.getChartSetupValues(var7, "slippage"));
      this.settings.set("CS_ROAM_MIN_DISTANCES", ProjectConfigHelper.getChartSetupValues(var7, "minDist"));
      this.settings.set("CS_ROAM_COMMISSIONS", ProjectConfigHelper.getChartSetupCommissions(var7));
      this.settings.set("CS_ROAM_SWAPS", ProjectConfigHelper.getChartSetupSwaps(var7));
      this.settings.set("CS_ROAM_USE_MAINTETS_DATE", ProjectConfigHelper.getChartSetupMainTestValues(var7, "dates"));
      this.readConditions(var1);
      Element var8 = XMLUtil.tryAddElement(var1, "AcceptanceSettings");
      this.settings.set("ConditionsCheckMarkets", XMLUtil.getNodeValue(var8, "Check", "all").equals("markets"));
      this.settings.set("ConditionsMinConds", XMLUtil.getNodeIntValue(var8, "MinConditions", 0));
      this.settings.set("ConditionsMinMarkets", XMLUtil.getNodeIntValue(var8, "MinMarkets", 0));
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      ChartSetups var9 = (ChartSetups)this.settings.get("AdditionalChartSetups");
      if (var9 != null && var9.size() >= 1) {
         String var10 = var1.getMainResultKey();
         Result var11 = var1.subResult(var10);
         SettingsMap var12 = var11.getSettings();
         ArrayList var13 = new ArrayList();
         ArrayList var14 = (ArrayList)this.settings.get("CS_ROAM_SLIPPAGES");
         ArrayList var15 = (ArrayList)this.settings.get("CS_ROAM_MIN_DISTANCES");
         ArrayList var16 = (ArrayList)this.settings.get("CS_ROAM_USE_MAINTETS_DATE");
         ArrayList var17 = (ArrayList)this.settings.get("CS_ROAM_COMMISSIONS");
         ArrayList var18 = (ArrayList)this.settings.get("CS_ROAM_SWAPS");

         for (int var19 = 0; var19 < var9.size() && !this.stopPauseEngine.isStopped(); var19++) {
            ChartSetup var20 = (ChartSetup)var9.get(var19);
            SettingsMap var21 = this.cloneSettings(var12);
            ChartSetup var22 = (ChartSetup)var21.get("BacktestChart");
            if (var7 != null) {
               String var23 = ((ChartDef)var22.getCharts().get(0)).getSymbol();
               String var24 = ((ChartDef)var22.getCharts().get(0)).getTimeframe();
               String var25 = L.t("Running additional market backtest on %s/%s for %s", new Object[]{var23, var24, var8});
               var7.setLastEvent(var25);
            }

            var22 = this.applyAdditionalChart(var22, var20, (String)var16.get(var19));
            var21.set("BacktestChart", var22);
            var21.set("Slippage", Double.parseDouble((String)var14.get(var19)));
            var21.set("MinDistance", Double.parseDouble((String)var15.get(var19)));
            var21.set("Commission", var17.get(var19));
            var21.set("Swap", var18.get(var19));
            ResultsGroup var30 = this.testWithThisSetup(var21, var22, var10);
            var13.add(var30);
         }

         if (var13.size() > 0) {
            for (int var26 = 0; var26 < var13.size(); var26++) {
               ResultsGroup var28 = (ResultsGroup)var13.get(var26);
               var1.mergeWith(var28);
            }

            var1.createPortfolioResult();
         }

         double var27 = var11.getFitness((byte)127);
         var1.portfolio().setFitness((byte)127, var27);
         var1.portfolio().setFitness((byte)10, var27);
         var1.portfolio().setFitness((byte)20, var27);
         var1.portfolio().setFitness((byte)11, var27);
         var1.portfolio().setFitness((byte)40, var27);
         return this.checkConditions(var1, var2);
      } else {
         return true;
      }
   }

   private ResultsGroup testWithThisSetup(SettingsMap var1, ChartSetup var2, String var3) throws Exception {
      Object var4 = null;
      if (var2.getBacktestEngine() != 1316847364 && var2.getBacktestEngine() != -1816889229) {
         ITradingSimulator var5 = TraderSimulators.getSimulator(var2);
         var4 = new BacktestEngine(var5);
         var4.setUsePreparedData(true);
      } else {
         var4 = new StockpickerBacktestEngine();
      }

      var4.setSingleThreaded(true);
      var4.setStopPauseEngine(this.stopPauseEngine);
      var4.registerProgressListener(new IBacktestProgressListener() {
         public void setProgress(int var1) {
            RetestOnAdditionalMarkets.this.setJobProgress(var1);
         }

         public void increaseProgressStep() {
         }
      });
      var4.addSetup(var1);
      String var8 = "AdditionalMarket: " + var2.getMainChart().getSymbol() + "/" + var2.getMainChart().getTimeframe();
      return var4.runBacktest(var8, var8, false).getResults();
   }

   private ChartSetup applyAdditionalChart(ChartSetup var1, ChartSetup var2, String var3) throws Exception {
      ArrayList var4 = var1.getCharts();
      ArrayList var5 = var2.getCharts();
      if (var4.size() != var5.size()) {
         throw new Exception(L.t("Additional charts don't have the same number of subcharts as main data!", new Object[0]));
      }

      ChartDef var6 = (ChartDef)var4.get(0);
      ChartDef var7 = (ChartDef)var5.get(0);
      DataInfo var8 = DataManager.getDataInfo(var7.getConnectionName(), var7.getSymbol());
      if (var8 == null) {
         throw new DataException(2, "Data for connection '" + var7.getConnectionName() + "' and symbol '" + var7.getSymbol() + "' cannot be found!");
      }

      long var9 = this.checkHistoryFrom(var8, var6.getHistoryFrom());
      long var11 = this.checkHistoryTo(var8, var6.getHistoryTo());
      if (var3.equals("false")) {
         var9 = this.checkHistoryFrom(var8, var7.getHistoryFrom());
         var11 = this.checkHistoryTo(var8, var7.getHistoryTo());
      }

      ChartSetup var13 = new ChartSetup(var7.getConnectionName(), var7.getSymbol(), var7.getTimeframe(), var9, var11, var7.getSpread(), var7.getSession());
      var13.setTestPrecision(var2.getTestPrecision());
      var13.setBacktestEngine(var1.getBacktestEngine());

      for (int var14 = 1; var14 < var4.size(); var14++) {
         var7 = (ChartDef)var5.get(var14);
         var13.addChart(var7.getConnectionName(), var7.getSymbol(), var7.getTimeframe());
      }

      return var13;
   }

   private long checkHistoryFrom(DataInfo var1, long var2) {
      return var2 >= var1.dateFrom && var2 < var1.dateTo ? var2 : var1.dateFrom;
   }

   private long checkHistoryTo(DataInfo var1, long var2) {
      return var2 <= var1.dateTo && var2 > var1.dateFrom ? var2 : var1.dateTo;
   }

   protected boolean checkConditions(ResultsGroup var1, int var2) {
      return this.settings.get("ConditionsCheckMarkets") ? this.checkAdditionalMarketConditions(var1, var2) : super.checkConditions(var1, var2);
   }

   protected boolean checkAdditionalMarketConditions(ResultsGroup var1, int var2) {
      int var3 = 0;

      try {
         var3 = var1.additionalMarketKeys().size();
      } catch (Exception var18) {
      }

      int[] var4 = new int[var3];
      ArrayList var5 = (ArrayList)this.settings.get("Conditions");
      boolean var6 = false;
      if (var5 != null && var5.size() > 0 && var3 > 0) {
         for (int var7 = 0; var7 < var5.size(); var7++) {
            Condition var8 = (Condition)var5.get(var7);
            if (var8.isUsed() && var8.isCrossCheckCondition(this.getSettingName())) {
               try {
                  this.conditionsChecker.leftValues.clear();
                  this.conditionsChecker.rightValues.clear();
                  byte[] var9 = this.conditionsChecker.getSampleType(var8.getLeftSideConditionValue());
                  byte[] var10 = this.conditionsChecker.getSampleType(var8.getRightSideConditionValue());
                  int[] var11 = this.conditionsChecker.getAdditionalMarket(var8.getLeftSideConditionValue(), var1);
                  int[] var12 = this.conditionsChecker.getAdditionalMarket(var8.getRightSideConditionValue(), var1);

                  for (int var13 = 0; var13 < var11.length; var13++) {
                     for (int var14 = 0; var14 < var12.length; var14++) {
                        boolean var15 = true;

                        for (int var16 = 0; var16 < var9.length && var15; var16++) {
                           for (int var17 = 0; var17 < var10.length; var17++) {
                              this.conditionsChecker.leftValues.put("SampleType", var9[var16]);
                              this.conditionsChecker.leftValues.put("AdditionalMarket", var11[var13]);
                              this.conditionsChecker.rightValues.put("SampleType", var10[var17]);
                              this.conditionsChecker.rightValues.put("AdditionalMarket", var12[var14]);
                              if (var8.isCrossCheckCondition("RetestOnAdditionalMarkets")) {
                                 var6 = true;
                              }

                              if (!var8.isMet(var1, this.conditionsChecker.leftValues, this.conditionsChecker.rightValues)) {
                                 var15 = false;
                                 break;
                              }
                           }
                        }

                        if (var15) {
                           if (var11[var13] > 0) {
                              var4[var11[var13] - 1]++;
                           }

                           if (var12[var14] > 0) {
                              var4[var12[var14] - 1]++;
                           }
                        }
                     }
                  }
               } catch (CrossCheckDataNotExistException var19) {
               } catch (Exception var20) {
                  Log.error("Error while checking condition", var20);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = L.t("Cross Check filter in '%s': Error evaluating conditions: %s", new Object[]{this.getName(), var20.getMessage()});
                  return false;
               }
            }
         }
      }

      if (var6) {
         int var21 = (Integer)this.settings.get("ConditionsMinConds");
         int var22 = (Integer)this.settings.get("ConditionsMinMarkets");
         int var23 = 0;

         for (int var24 = 0; var24 < var4.length; var24++) {
            if (var4[var24] >= var21) {
               var23++;
            }
         }

         if (var23 < var22) {
            this.dismissalReason = 10012;
            this.dismissalMessage = L.t(
               "Cross Check filter in '%s': %s",
               new Object[]{this.getName(), L.t("Failed - less than %d conditions passed on %d markets", new Object[]{var21, var22})}
            );
            return false;
         }
      }

      return true;
   }

   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      int var5 = this.getMarketIndex(var3, var4);
      byte var6 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var7 = this.getSampleType(var3, var4);
      byte var8 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      List var9 = var1.additionalMarketKeys();
      if (var9.size() > 0 && var5 < var9.size()) {
         String var10 = (String)var9.get(var5);
         DatabankColumn var11 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
         return var11.getNumericValue(var1, var10, var6, var8, var7);
      } else {
         throw new CrossCheckDataNotExistException(this.getName());
      }
   }

   private int getMarketIndex(Element var1, Object... var2) {
      int var3 = XMLUtil.getIntAttr(var1, "market", 1);
      if (var3 == 0 && var2.length > 1 && var2[1] instanceof HashMap) {
         HashMap var4 = (HashMap)var2[1];
         if (var4.containsKey("AdditionalMarket")) {
            var3 = (Integer)var4.get("AdditionalMarket");
         }
      }

      return var3 - 1;
   }

   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      int var5 = this.getMarketIndex(var3, var4);
      List var6 = var1.additionalMarketKeys();
      if (var6.size() > 0 && var5 < var6.size()) {
         String var7 = (String)var6.get(var5);
         byte var8 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
         byte var9 = this.getSampleType(var3, var4);
         byte var10 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
         DatabankColumn var11 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
         SQStats var12 = var11.getStats(var1, var7, var8, var10, var9);
         return var12 != null;
      } else {
         return false;
      }
   }

   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      int var5 = this.getMarketIndex(var3, var4);
      byte var6 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var7 = this.getSampleType(var3, var4);
      byte var8 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      List var9 = var1.additionalMarketKeys();
      if (var9.size() > 0 && var5 < var9.size()) {
         String var10 = (String)var9.get(var5);
         DatabankColumn var11 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
         return var11.getValue(var1, var10, var6, var8, var7);
      } else {
         throw new CrossCheckDataNotExistException(this.getName());
      }
   }

   public String getColumnTitle(String var1, Element var2, Object... var3) {
      int var4 = this.getMarketIndex(var2, var3) + 1;
      String var5 = var4 == 0 ? "Every market" : "Market " + var4;
      byte var6 = XMLUtil.getByteAttr(var2, "direction", (byte)0);
      byte var7 = this.getSampleType(var2, var3);
      byte var8 = XMLUtil.getByteAttr(var2, "plType", (byte)10);
      String var9 = this.getColumnTitleTemplate();
      var9 = var9.replace("#columnName#", var1);
      var9 = var9.replace("#market#", var5);
      var9 = var9.replace(", #sampleType#", var7 == 127 ? "" : ", " + SampleTypes.typeToString(var7));
      var9 = var9.replace(", #direction#", var6 == 0 ? "" : ", " + Directions.typeToString(var6));
      return var9.replace(", #plType#", var8 == 10 ? "" : ", " + PlTypes.typeToString(var8));
   }

   public String getColumnTitleTemplate() {
      return "#columnName# (" + this.getShortName() + ", #market#, #sampleType#, #direction#, #plType#)";
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
      double var9 = var8.getNumericValue(var1, "Portfolio", var2, (byte)10, var3);
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
         RetestOnAdditionalMarkets.Goal var7 = this.weightedGoals.get(var6);
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
      return "Portfolio";
   }

   public void initFitnessFromXml(Element var1) {
      this.weightedGoals.clear();
      Element var2 = var1.getChild("Ranking");
      this.databankColumnName = var2.getAttributeValue("type");

      for (Element var4 : var2.getChildren("Goal")) {
         RetestOnAdditionalMarkets.Goal var5 = new RetestOnAdditionalMarkets.Goal();
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
      throw new Exception(L.t("This fitness method cannot be used in Walk-Forward!", new Object[0]));
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      ChartSetups var2 = (ChartSetups)this.settings.get("AdditionalChartSetups");

      for (ChartSetup var4 : var2) {
         var4.setBacktestEngine(var1.getBacktestEngine());
      }

      return var2;
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
      List var4 = var2.getChild("Setups").getChildren("Setup");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         Element var7 = var6.getChild("Chart");
         var3 = var3 + L.t("Additional backtest (%d): ", new Object[]{var5 + 1});
         var3 = var3 + L.t("Symbol: %s", new Object[]{var7.getAttributeValue("symbol")});
         var3 = var3 + "," + L.t("Timeframe: %s", new Object[]{var7.getAttributeValue("timeframe")});
         var3 = var3 + "," + L.t("Start date: %s", new Object[]{var6.getAttributeValue("dateFrom")});
         var3 = var3 + "," + L.t("End date: %s", new Object[]{var6.getAttributeValue("dateTo")});
         var3 = var3 + "," + L.t("Test precision: %s", new Object[]{Precisions.toString(Integer.parseInt(var6.getAttributeValue("testPrecision")))});
         var3 = var3 + "," + L.t("Spread: %s", new Object[]{var7.getAttributeValue("spread")});
         var3 = var3 + "," + L.t("Slippage: %s", new Object[]{var6.getAttributeValue("slippage")});
         var3 = var3 + "," + L.t("Min distance: %s", new Object[]{var6.getAttributeValue("minDist")});
         String var8 = ProjectConfigHelper.getCommissionsMethod(var6).printFormatedName();
         var3 = var3 + "," + L.t("Commission: %s", new Object[]{var8});
         String var9 = ProjectConfigHelper.getSwapMethod(var6).printFormatedName();
         var3 = var3 + "," + L.t("Swap: %s", new Object[]{var9});
         List var10 = var6.getChildren("Chart");
         String var11 = var10.size() - 1 + "";
         if (var10.size() > 0) {
            for (int var12 = 1; var12 < var10.size(); var12++) {
               var3 = var3 + "\n";
               var3 = var3 + L.t("Subcharts (%d): ", new Object[]{var12});
               var3 = var3
                  + String.format("%s, %s", ((Element)var10.get(var12)).getAttributeValue("symbol"), ((Element)var10.get(var12)).getAttributeValue("timeframe"));
            }
         }

         if (var5 < var4.size() - 1) {
            var3 = var3 + "\n\n";
         }
      }

      var3 = var3 + "\n";
      Element var27 = var1.getChild("AcceptanceSettings").getChild("Conditions");
      ArrayList var28 = ProjectConfigHelper.getConditions(var27);

      for (int var29 = 0; var29 < var28.size(); var29++) {
         Condition var30 = (Condition)var28.get(var29);
         if (var30.isUsed()) {
            var3 = var3 + var30.toString();
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
      return 10033;
   }

   public IFitnessFunction clone() {
      return this;
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
