package com.strategyquant.plugin.Results.impl.EquityChart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.equitychart.Equity;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.equitychart.MaxNewHighDuration;
import com.strategyquant.tradinglib.equitychart.Periods;
import com.strategyquant.tradinglib.equitychart.Points;
import com.strategyquant.tradinglib.equitychart.SampleMarkers;
import com.strategyquant.tradinglib.equitychart.Stagnation;
import com.strategyquant.tradinglib.equitychart.TrendLines;
import com.strategyquant.tradinglib.equitychart.WalkForward;
import com.strategyquant.tradinglib.equitychart.YearMarkers;
import com.strategyquant.tradinglib.equitychartnew.addons.IEquityChartAddon;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByCloseTime;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquityChartServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(EquityChartServlet.class);
   private static final DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyy-MM-dd");
   private static final String LOCK_EQUITYCHARTSERVLET = "EquityChartServlet";
   private EquityChart chart = null;
   private Periods periods = null;
   private YearMarkers yearMarkers = null;
   private SampleMarkers sampleMarkers = null;
   private TrendLines trendLines = null;
   private Points points = null;
   private Stagnation stagnation = null;
   private MaxNewHighDuration maxNewHighDuration = null;
   private WalkForward walkForward = null;
   private Equity equity = null;
   private IResultsGroupProvider rgProvider;
   private final Comparator<Order> comparatorByCloseTime = new OrderComparatorByCloseTime();
   private final Comparator<Order> comparatorByOpenTime = new OrderComparatorByOpenTime();
   private static final String SettingKey = "EquityChart";
   public ResultsGroup rg = null;

   public EquityChartServlet(IResultsGroupProvider var1) {
      this.chart = new EquityChart();
      this.periods = new Periods();
      this.yearMarkers = new YearMarkers();
      this.sampleMarkers = new SampleMarkers();
      this.trendLines = new TrendLines();
      this.points = new Points();
      this.stagnation = new Stagnation();
      this.maxNewHighDuration = new MaxNewHighDuration();
      this.walkForward = new WalkForward();
      this.equity = new Equity();
      this.rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONObject loadLastSettings() throws Exception {
      JSONObject var1 = new JSONObject();
      String var2 = MainApp.settings().get("EquityChart", null);
      if (var2 != null) {
         String[] var3 = var2.split(",");
         if (var3.length == 11) {
            var1.put("xAxisType", var3[0]);
            var1.put("drawdown", var3[1]);
            var1.put("volume", var3[2]);
            var1.put("stagnation", var3[3]);
            var1.put("maxNewHighDuration", var3[4]);
            var1.put("equity", var3[5]);
            var1.put("yearmarker", var3[6]);
            var1.put("trendline", Boolean.valueOf(var3[7]));
            var1.put("points", var3[8]);
            var1.put("dailychart", var3.length > 8 ? Boolean.valueOf(var3[9]) : false);
            var1.put("volatility", var3.length > 8 ? Boolean.valueOf(var3[10]) : false);
            var1.put("benchmarkActive", var3.length == 12 ? Boolean.valueOf(var3[11]) : false);
            var1.put("benchmarkSymbol", var3.length == 13 ? var3[12] : "SPY.D");
            var1.put("benchmarkNormalization", var3.length == 12 ? var3[11] : "exposure");
         }
      }

      return var1;
   }

   private synchronized String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"direction", "sampleType", "width"});
      ResultsGroup var2 = null;
      JsonCreator var3 = new JsonCreator();

      try {
         String var4 = null;
         String var5 = this.tryGetParam(var1, "resultKey")[0];
         if ("Portfolio only".equals(var5) || "Portfolio parts only".equals(var5)) {
            var4 = var5;
            var5 = "Portfolio";
         }

         byte var6 = Byte.parseByte(((String[])var1.get("direction"))[0]);
         byte var7 = Byte.parseByte(((String[])var1.get("sampleType"))[0]);
         String var8 = ((String[])var1.get("xaxis"))[0].toString();
         byte var9 = Byte.parseByte(((String[])var1.get("drawdown"))[0]);
         byte var10 = Byte.parseByte(((String[])var1.get("volume"))[0]);
         byte var11 = Byte.parseByte(((String[])var1.get("stagnation"))[0]);
         byte var12 = Byte.parseByte(var1.containsKey("maxNewHighDuration") ? ((String[])var1.get("maxNewHighDuration"))[0] : "0");
         String var13 = ((String[])var1.get("equity"))[0].toString();
         Boolean var14 = true;
         Boolean var15 = Boolean.parseBoolean(((String[])var1.get("trendline"))[0]);
         String var16 = ((String[])var1.get("points"))[0].toString();
         int var17 = Integer.parseInt(((String[])var1.get("width"))[0]);
         boolean var18 = Boolean.parseBoolean(((String[])var1.get("zoom"))[0]);
         int var19 = Integer.parseInt(((String[])var1.get("zoomwidth"))[0]);
         boolean var20 = "time".equals(var8) || "Portfolio".equals(var5);
         long var21 = var20 ? this.parseTime(((String[])var1.get("zoomminX"))[0]) : this.parseLong(((String[])var1.get("zoomminX"))[0]);
         long var23 = var20 ? this.parseTime(((String[])var1.get("zoommaxX"))[0]) : this.parseLong(((String[])var1.get("zoommaxX"))[0]);
         boolean var25 = Boolean.parseBoolean(((String[])var1.get("dailychart"))[0]);
         boolean var26 = Boolean.parseBoolean(((String[])var1.get("volatility"))[0]);
         boolean var27 = Boolean.parseBoolean(((String[])var1.get("benchmarkActive"))[0]);
         String var28 = ((String[])var1.get("benchmarkSymbol"))[0].toString();
         String var29 = ((String[])var1.get("benchmarkNormalization"))[0].toString();
         this.saveLastSettings(var8, var9, var10, var11, var12, var13, var14, var15, var16, var25, var26, var27, var28, var29);
         if (!var18) {
            var19 = var17;
         }

         if (var19 < 1024) {
            var19 = 1024;
         }

         var3.beginObject();
         this.chart.reset();
         this.chart.setPLType((byte)10);
         this.chart.zoom(var18, var19, var21, var23);
         if (var5 != null) {
            String[] var30 = var5.split(":");
            this.chart.setCornerCaption(var30.length == 2 ? var30[1] : var5);
         }

         if (this.rg == null) {
            var2 = this.rgProvider.get(var1, "EquityChartServlet");
         } else {
            var2 = this.rg;
         }

         OrdersList var46 = var2.orders().filterExcludingControlOrders(var5, var6, var7, true);
         var46.sort(this.comparatorByCloseTime);
         this.chart.setDomainAxisType(var8);
         if (!var5.equals("Portfolio")) {
            MainChartDataset var47 = EquityChartUtils.generateDataset(var46, var8);
            var47.title = "Balance";
            var47.filled = true;
            var47.fillColor = "#DAF5FF";
            var47.fillNegativeColor = "#F49C9C";
            var47.fillColorDark = "#333C63";
            var47.thickness = 1;
            this.chart.addMainChartDataset(var47);
            var3.put("portfolio", false, true);
         } else {
            String var31 = var2.getMainResultKey();
            List var32 = EquityChartUtils.generatePorfolioDatasets(var2, var6, var7, var4, var8);

            for (int var33 = 0; var33 < var32.size(); var33++) {
               MainChartDataset var34 = (MainChartDataset)var32.get(var33);
               this.chart.addMainChartDataset(var34);
               if (var34.name.equals("Portfolio")) {
                  var34.thickness = 3;
                  var34.filled = true;
                  var34.fillColor = "#DAF5FF";
                  var34.fillColorDark = "#333C63";
                  var34.fillNegativeColor = "#F49C9C";
               } else if (var4 != null && var4.equals("Portfolio parts only") && var34.name.equals(var31) && var31.startsWith("Main:")) {
                  var34.thickness = 3;
               } else if (var4 == null && var34.name.equals(var31) && var31.startsWith("Main:")) {
                  var34.thickness = 3;
               }
            }

            var3.put("portfolio", true, true);
         }

         var3.put("stockPick", var2 != null && var2.isStockpickerStrategy(), true);
         var46.sort(this.comparatorByOpenTime);
         boolean var48 = true;
         List var49 = this.periods.getAvailablePeriods(var46, var8);
         if (var49.size() > 10 && var5.equals("Portfolio")) {
            var48 = false;
         }

         var46.sort(this.comparatorByCloseTime);
         if (var14) {
            this.yearMarkers.print(this.chart, var46, var8);
         }

         if ((var7 == 127 || var7 == 10) && !this.isWFResult(var2, var5) && var48) {
            this.sampleMarkers.print(this.chart, var49, true);
         }

         if (var15 && var48) {
            this.trendLines.print(this.chart, var49, var8);
         }

         if (!var16.equals("off")) {
            this.points.print(this.chart, var16, var8);
         }

         if (var4 == null || !var4.equals("Portfolio parts only")) {
            this.equity.print(this.chart, var46, var2, var8, var5, var6, var7, var13);
         }

         if (var11 != 0) {
            this.stagnation.print(this.chart, var2, var5, var46, var6, (byte)10, var11, var8);
         }

         if (var12 != 0) {
            this.maxNewHighDuration.print(this.chart, var2, var5, var46, var6, (byte)10, var12, var8);
         }

         for (IEquityChartAddon var51 : SQPluginManager.getPlugins(IEquityChartAddon.class)) {
            try {
               var51.print(this.chart, var2, var5, var46, var1, var8);
            } catch (Exception var41) {
               Log.error("Error while rendering EquityChart addon '" + var51.getName() + "'. Exc.", var41);
            }
         }

         this.walkForward.print(this.chart, var46, var2, var8, var5, var6, var7);
      } catch (RecordNotFoundException var42) {
         Log.info("onPrint: " + var42.getMessage());
         return apiErrorJSONNoLog(null, var42);
      } catch (Exception var43) {
         return apiErrorJSON(L.t("Loading equity chart failed", new Object[0]), var43);
      } finally {
         if (var2 != null && this.rg == null) {
            var2.releaseLock("EquityChartServlet");
            Object var45 = null;
            this.rg = null;
         }
      }

      var3.putBeginObject("chart");
      this.chart.fillJSON(var3);
      var3.endObject(true);
      var3.put("success", "ok", false);
      var3.endObject(false);
      return var3.toJson();
   }

   private void saveLastSettings(
      String var1,
      byte var2,
      byte var3,
      byte var4,
      byte var5,
      String var6,
      Boolean var7,
      Boolean var8,
      String var9,
      Boolean var10,
      Boolean var11,
      Boolean var12,
      String var13,
      String var14
   ) {
      String var15 = var1
         + ","
         + var2
         + ","
         + var3
         + ","
         + var4
         + ","
         + var5
         + ","
         + var6
         + ","
         + var7
         + ","
         + var8
         + ","
         + var9
         + ","
         + var10
         + ","
         + var11
         + ","
         + var12
         + ","
         + var13
         + ","
         + var14;
      MainApp.settings().set("EquityChart", var15);
   }

   private boolean isWFResult(ResultsGroup var1, String var2) throws Exception {
      WalkForwardMatrixResult var3 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
      if (var3 == null) {
         return false;
      }

      WalkForwardResult var4 = var3.getWFResult(var2, true);
      return var4 != null;
   }

   private long parseLong(String var1) {
      try {
         return Long.parseLong(var1);
      } catch (Exception var3) {
         return -1L;
      }
   }

   private long parseTime(String var1) {
      try {
         return formaterDate.parseMillis(var1);
      } catch (Exception var3) {
         return Long.parseLong(var1);
      }
   }
}
