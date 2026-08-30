package com.strategyquant.plugin.EquityChart.impl.Benchmark;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.HideDatabankChoice;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.charts.linechart.series.XYValue;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.equitychart.BenchmarkTable;
import com.strategyquant.tradinglib.equitychart.ChartDataset;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.equitychart.SubChart;
import com.strategyquant.tradinglib.equitychart.SubChartDataset;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.results.stats.computer.OrderPLComputer;
import it.unimi.dsi.fastutil.longs.Long2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tamas Takacs")
@Name(name = "EquityChart Benchmark plugin")
@Category(name = "EquityChart")
@License(text = "")
@ShortDesc(text = "Short description")
@HideDatabankChoice
@PluginImplementation
public class Benchmark implements IEquityChartAddon {
   private static final String NormOff = "off";
   private static final String NormDD = "drawdown";
   private static final String NormDDPct = "drawdownPct";
   private static final String NormMM = "moneyManagement";
   private static final String NormExposure = "exposure";
   private static final Logger Log = LoggerFactory.getLogger(Benchmark.class);

   public String getProduct() {
      return "SQUANTAlgoWizardBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 300;
   }

   public void initPlugin() throws Exception {
   }

   public String getName() {
      return "Benchmark";
   }

   public void print(EquityChart var1, ResultsGroup var2, String var3, OrdersList var4, Map<String, String[]> var5, String var6) throws Exception {
      boolean var7 = Boolean.parseBoolean(((String[])var5.get("benchmarkActive"))[0]);
      if (var7) {
         if (var6.equals("time")) {
            try {
               BenchmarkTable var8 = var1.benchmark;
               String var9 = ((String[])var5.get("benchmarkSymbol"))[0];
               String var10 = ((String[])var5.get("benchmarkNormalization"))[0];
               boolean var11 = false;
               if (MainApp.isBacktestNode()) {
                  var9 = var9.replace("_benchmark", "");
               }

               DataInfo var12 = DataManager.getDataInfo("History", var9);
               if (var12 == null) {
                  Log.info(String.format("Cannot print benchmark, missing '%s' data.", var9));
               } else {
                  byte var13 = Byte.parseByte(((String[])var5.get("direction"))[0]);
                  byte var14 = Byte.parseByte(((String[])var5.get("sampleType"))[0]);
                  long[] var15 = EquityChartUtils.dateRange(var4, var2.isStockpickerStrategy());
                  long var16 = var15[0];
                  long var18 = var15[1];
                  Log.info(String.format("SPY benchmark from %s to %s ...", SQTime.toDateString(var16), SQTime.toDateString(var18)));
                  double var20 = 2.5;
                  String var22 = "No Session";
                  ChartDef var23 = new ChartDef("History", var9, "D1", var16, var18, var20, var22);
                  IDataLoader var24 = DataManager.getDataLoader(var23, 1, null);
                  var24.open();
                  ObjectArrayList var25 = new ObjectArrayList();

                  while (var24.hasNextTick()) {
                     VersatileData var26 = new VersatileData();
                     var24.getNextTick(var26);
                     var25.add(new XYValue(var26.time, var26.close));
                  }

                  double var57 = 1.0;
                  SettingsMap var28 = var2.portfolio().getSettings();
                  Element var29 = XMLUtil.stringToElement(var2.getLastSettings());
                  Element var30 = var29.getChild("RiskMoneyManagement");
                  MoneyManagementMethod var31 = ProjectConfigHelper.getMoneyManagement(var30.getChild("MoneyManagement"));
                  double var32 = var31.getInitialCapital();
                  SQStats var36 = var2.portfolio().stats(var13, (byte)10, var14);
                  double var37 = var36.getDouble("Exposure", 1.0);
                  if (var37 == 0.0) {
                     var37 = 1.0;
                  }

                  double var39 = var36.getDouble("Drawdown", 1.0);
                  double var41 = var36.getDouble("DrawdownPct", 1.0);
                  MainChartDataset var43 = new MainChartDataset("benchmark", var25.size());
                  var43.title = "SPY";
                  var43.color = "#ffb347";
                  var43.thickness = 3;
                  var1.addMainChartDataset(var43);
                  Benchmark.NormalizedData var44 = this.normalizeByExposure(var25, var2, null, var12, var32, 1.0, 100.0);
                  double var45 = this.calculateCorrelationStrategySPY(var1, var44.equityMap);
                  Benchmark.NormalizedData var47 = null;
                  this.printHeader(var1, var11);
                  double var48 = var36.getDouble("NetProfit", 1.0);
                  this.printLine(var1, "Strategy", "#3366CC", 85, var48, var39, var41, var37, var45, var11);
                  Benchmark.NormalizedData var50 = this.computeNormalizedByPctDD(var41, var44, var25, var2, var12, var32);
                  if (var10.equals("off")) {
                     this.printLine(var1, "SPY", "#A9A9A9", 110, var44.equitySPY, var44.dd, var44.ddPct, 100.0, 0.0, var11);
                     this.fillDatasetValues(var43, var25, var44.equityMap);
                     var47 = var44;
                  } else if (var10.equals("drawdown")) {
                     var57 = var39 / var44.dd;
                     Benchmark.NormalizedData var51 = this.normalizeByExposure(var25, var2, null, var12, var32, var57, 100.0);
                     this.printLine(var1, "SPY (norm. $ drawdown)", "#A9A9A9", 160, var51.equitySPY, var51.dd, var51.ddPct, 100.0, 0.0, var11);
                     this.fillDatasetValues(var43, var25, var51.equityMap);
                     var47 = var51;
                  } else if (var10.equals("drawdownPct")) {
                     this.printLine(var1, "SPY (norm. $ drawdown)", "#A9A9A9", 160, var50.equitySPY, var50.dd, var50.ddPct, 100.0, 0.0, var11);
                     this.fillDatasetValues(var43, var25, var50.equityMap);
                     var47 = var50;
                  } else if (var10.equals("moneyManagement")) {
                     Benchmark.NormalizedData var59 = this.normalizeByExposure(var25, var2, var31, var12, var32, 1.0, 100.0);
                     this.printLine(var1, "SPY (norm. MM)", "#A9A9A9", 160, var59.equitySPY, var59.dd, var59.ddPct, 100.0, 0.0, var11);
                     this.fillDatasetValues(var43, var25, var59.equityMap);
                     var47 = var59;
                  } else if (var10.equals("exposure")) {
                     double var34 = var32 / 100.0 * var37;
                     Benchmark.NormalizedData var60 = this.normalizeByExposure(var25, var2, null, var12, var34, 1.0, var37);
                     double var52 = var60.dd;
                     this.printLine(var1, "SPY (norm. exposure)", "#A9A9A9", 135, var60.equitySPY, var60.dd, var60.ddPct, var37, 0.0, var11);
                     this.fillDatasetValues(var43, var25, var60.equityMap);
                     var47 = var60;
                  }

                  if (var47 == null) {
                     throw new Exception("Normalization method was not recognized?!");
                  }

                  SQStats var61 = var47.calculateStats(var2, var29, var13, (byte)10, var14);
                  SQStats var62 = var50.calculateStats(var2, var29, var13, (byte)10, var14);
                  var8.fill(var2, var31, var9, var36, var45, var10, var47.initBuyAmount, var61, var62);
                  byte var53 = Byte.parseByte(((String[])var5.get("drawdown"))[0]);
                  SubChart var54 = var1.findSubChartByKey("Drawdown");
                  if (var54 != null && (var53 == 10 || var53 == 40 || var53 == 20 || var53 == 50)) {
                     SubChartDataset var55 = new SubChartDataset("DrawdownBenchmark", var47.ddMapMoney.size());
                     var55.setName("Drawdown Benchmark");
                     var55.type = "line";
                     var55.color = "#ffb347";
                     var55.thickness = 3;
                     var54.addDataset(var55);
                     if (var53 == 10 || var53 == 40) {
                        this.fillDatasetValues(var55, var25, var47.ddMapMoney);
                     } else if (var53 == 20 || var53 == 50) {
                        this.fillDatasetValues(var55, var25, var47.ddMapPct);
                     }
                  }

                  MainChartDataset var63 = (MainChartDataset)var1.getMainChartDatasets().get(0);
                  var63.thickness = 3;
               }
            } catch (Exception var56) {
               throw new Exception("Error while printing SPY benchmark. Exc.", var56);
            }
         }
      }
   }

   private Benchmark.NormalizedData computeNormalizedByPctDD(
      double var1, Benchmark.NormalizedData var3, ObjectArrayList<XYValue> var4, ResultsGroup var5, DataInfo var6, double var7
   ) throws Exception {
      Benchmark.NormalizedData var9 = new Benchmark.NormalizedData();
      var1 = SQUtils.round2(var1);
      double var10 = var1 / var3.ddPct;
      Benchmark.NormalizedData var12 = this.normalizeByExposure(var4, var5, null, var6, var7, var10, 100.0);
      double var13 = var1 - var12.ddPct;
      if (Math.abs(var13) < 0.015) {
         Log.info("Matching PctDD found in cycles: 0");
         return var12;
      }

      double var15 = var10;
      double var17 = var13;
      double var19 = 0.2;
      int var21 = 0;

      for (int var22 = 0; var22 < 30; var22++) {
         var21 = var22;
         if (var17 > 0.0) {
            var10 = var15 * (1.0 + var19);
         } else {
            var10 = var15 * (1.0 - var19);
         }

         var12 = this.normalizeByExposure(var4, var5, null, var6, var7, var10, 100.0);
         var17 = var1 - var12.ddPct;
         if (SQUtils.round2(var12.ddPct) == var1 || Math.abs(var17) <= 0.01) {
            Log.debug("Matching PctDD found in cycles: {}", var21);
            return var12;
         }

         if (Math.abs(var17) < Math.abs(var13)) {
            var13 = Math.abs(var17);
            var15 = var10;
            var9 = var12;
         } else {
            var19 /= 2.0;
         }
      }

      Log.info("Matching PctDD found in cycles: {}", var21);
      return var9;
   }

   private Benchmark.NormalizedData normalizeByExposure(
      ObjectArrayList<XYValue> var1, ResultsGroup var2, MoneyManagementMethod var3, DataInfo var4, double var5, double var7, double var9
   ) throws Exception {
      Benchmark.NormalizedData var11 = new Benchmark.NormalizedData();
      Long2DoubleAVLTreeMap var12 = new Long2DoubleAVLTreeMap();
      var11.equityMap = var12;
      var11.ddMapMoney = new Long2DoubleAVLTreeMap();
      var11.ddMapPct = new Long2DoubleAVLTreeMap();
      var11.ddMapPips = new Long2DoubleAVLTreeMap();
      OrdersList var13 = new OrdersList("normalized");
      var11.ordersList = var13;
      double var14 = 0.0;
      long var16 = 0L;
      double var18 = 0.0;
      double var20 = 0.0;
      double var26 = 9.9999999E7;
      double var28 = 9.9999999E7;
      double var30 = var5;
      double var32 = var5;

      for (int var34 = 0; var34 < var1.size(); var34++) {
         long var35 = SQTime.getDateInMs(((XYValue)var1.get(var34)).x);
         double var37 = ((XYValue)var1.get(var34)).y;
         if (var34 == 0) {
            var14 = var37;
            var16 = var35;

            try {
               if (var3 != null) {
                  StrategyBase var39 = StrategyBase.createXmlStrategy(var2.getStrategyXml());
                  var39.accountBalance = var5;
                  var39.accountEquity = var5;
                  var18 = var3.computeTradeSize(var39, var4.symbol, (byte)1, var14, 0.0, var4.symbolInfo.tickSize, var4.symbolInfo.pointValue);
               } else {
                  var18 = var5 / var14;
               }

               var11.initBuyAmount = var18 * var14 / 100.0 * var9 * var7;
            } catch (Exception var40) {
               throw new Exception("Failed to calculate initial buy amount from MoneyManagement method - " + var40.getMessage(), var40);
            }
         }

         var20 = (var37 - var14) * var18 * var7;
         var12.put(var35, var20);
         Order var41 = new Order();
         var41.Type = 1;
         var41.PL = (float)(var5 + var20 - var32);
         var41.PctPL = (float)OrderPLComputer.getPercentagePL(var41.PL, var5);
         var41.OpenPrice = (float)var14;
         var41.ClosePrice = (float)var37;
         var41.OpenTime = var16;
         var41.CloseTime = var35;
         var13.add(var41);
         var32 = var5 + var20;
         double var22 = (float)this.specialSubtraction(var30, var32);
         double var24 = (float)this.getPercentageDD(var22, var30);
         var11.ddMapMoney.put(SQTime.getDateInMs(var35), var22);
         var11.ddMapPct.put(SQTime.getDateInMs(var35), var24);
         if (var24 < var26) {
            var26 = var24;
         }

         if (var22 < var28) {
            var28 = var22;
         }

         if (var32 > var30) {
            var30 = var32;
         }
      }

      var11.equitySPY = var20;
      var11.dd = SQUtils.round2(Math.abs(var28));
      var11.ddPct = SQUtils.round2(Math.abs(var26));
      return var11;
   }

   private void printHeader(EquityChart var1, boolean var2) {
      if (var2) {
         var1.drawText("Benchmark", "#A9A9A9", "11px Arial", 130, 60);
         var1.drawText("Profit", "#A9A9A9", "11px Arial", 330, 60);
         var1.drawText("Max DD", "#A9A9A9", "11px Arial", 480, 60);
         var1.drawText("Max DD %", "#A9A9A9", "11px Arial", 580, 60);
         var1.drawText("Return/DD", "#A9A9A9", "11px Arial", 680, 60);
         var1.drawText("Exposure", "#A9A9A9", "11px Arial", 780, 60);
         var1.drawText("Corr. with SPY", "#A9A9A9", "11px Arial", 880, 60);
      }
   }

   private void printLine(
      EquityChart var1, String var2, String var3, int var4, double var5, double var7, double var9, double var11, double var13, boolean var15
   ) {
      if (var15) {
         double var16 = SQUtils.round2(SQUtils.safeDivide(var5, var7));
         var1.drawText(var2, var3, "15px Arial", 130, var4);
         var1.drawText(PlTypes.printPL(var5, (byte)10), var3, "15px Arial", 330, var4);
         var1.drawText(PlTypes.printPL(var7, (byte)10), var3, "15px Arial", 480, var4);
         var1.drawText(PlTypes.printPL(var9, (byte)20), var3, "15px Arial", 580, var4);
         var1.drawText(SQUtils.d2(var16), var3, "15px Arial", 680, var4);
         var1.drawText(PlTypes.printPL(var11, (byte)20), var3, "15px Arial", 780, var4);
         if (var13 != 0.0) {
            var1.drawText(SQUtils.d2(var13), var3, "15px Arial", 880, var4);
         }
      }
   }

   private void fillDatasetValues(ChartDataset var1, ObjectArrayList<XYValue> var2, Long2DoubleAVLTreeMap var3) {
      for (int var6 = 0; var6 < var2.size(); var6++) {
         long var4 = ((XYValue)var2.get(var6)).x;
         double var7 = var3.get(var4);
         var1.addValue(var4, var7);
      }
   }

   private double calculateCorrelationStrategySPY(EquityChart var1, Long2DoubleAVLTreeMap var2) {
      MainChartDataset var3 = (MainChartDataset)var1.getMainChartDatasets().get(0);
      long[] var4 = var3.xVals;
      double[] var5 = var3.yVals;
      double[] var6 = new double[var5.length];

      for (int var7 = 0; var7 < var4.length; var7++) {
         if (var2.containsKey(var4[var7])) {
            var6[var7] = var2.get(var4[var7]);
         }
      }

      return CorrelationComputer.calculateCorrelation(var5, var6);
   }

   private double specialSubtraction(double var1, double var3) {
      double var5 = var1 - var3;
      if (var5 < 0.0) {
         var5 = 0.0;
      }

      return -1.0 * var5;
   }

   private double getPercentageDD(double var1, double var3) {
      return !(var3 <= 0.0) && !(var1 > var3) ? var1 / (var3 / 100.0) : -1.0;
   }

   private class NormalizedData {
      public Long2DoubleAVLTreeMap equityMap;
      public double equitySPY;
      public double dd;
      public double ddPct;
      public OrdersList ordersList;
      public double initBuyAmount;
      public Long2DoubleAVLTreeMap ddMapMoney;
      public Long2DoubleAVLTreeMap ddMapPct;
      public Long2DoubleAVLTreeMap ddMapPips;

      private NormalizedData() {
      }

      public SQStats calculateStats(ResultsGroup var1, Element var2, byte var3, byte var4, byte var5) throws Exception {
         StatsComputer var6 = new StatsComputer();
         var6.initializeWithOrders(this.ordersList, true);
         StatsTypeCombination var7 = new StatsTypeCombination(var3, var4, var5);
         SettingsMap var8 = var1.portfolio().getSettings();
         ChartSetup var9 = (ChartSetup)var8.get("BacktestChart");
         if (var9 == null) {
            try {
               Element var10 = var2.getChild("Data");
               ChartSetups var11 = ProjectConfigHelper.getChartSetups(var10);
               var8.set("BacktestChart", var11.getMainSetup());
            } catch (Exception var12) {
               Benchmark.Log.error("Failed to obtain ChartSetups object from  RG settings.", var12);
            }
         }

         return var6.computeStats(null, var7, var8);
      }
   }
}
