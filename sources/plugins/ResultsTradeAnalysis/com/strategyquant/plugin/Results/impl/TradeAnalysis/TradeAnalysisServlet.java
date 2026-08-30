package com.strategyquant.plugin.Results.impl.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.tradeanalysis.TradeAnalysisChartsList;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeAnalysisServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(TradeAnalysisServlet.class);
   private static final String LOCK_TRADEANALYSISSERVLET = "TradeAnalysisServlet";
   private static IResultsGroupProvider rgProvider;
   private AnnualStatsComputer statsComputer;
   private static final String SettingKey = "TradeAnalysisCharts";
   public ResultsGroup rg = null;

   public TradeAnalysisServlet(IResultsGroupProvider var1) {
      this.statsComputer = new AnnualStatsComputer();
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "getAnnualStats":
            return this.onGetAnnualStats(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONArray loadLastSettings() throws Exception {
      String var1 = MainApp.settings().get("TradeAnalysisCharts", null);
      if (var1 == null || var1.split(",").length < 12) {
         var1 = "PLbyHourChart,PLbyWeekdayChart,TradesByWeekdayChart,LongShortProfitLossChart,WinLossCountByWeekdayChart,WinLossPLByMonthChart,LongShortTradesChart,PLGrowthByDurationChart,TradesByHourChart,PLLongChart,PLShortChart,TradesByDurationChart";
      }

      return SQUtils.toJSONArray(var1);
   }

   public JSONArray getTypes() throws Exception {
      JSONArray var1 = new JSONArray();

      for (TradeAnalysisChart var3 : TradeAnalysisChartsList.getInstance().getAvailableClasses()) {
         String var4 = var3.getClass().getSimpleName();
         if (!var4.equals("PLbyYearChart")) {
            JSONObject var5 = new JSONObject();
            var5.put("type", var4);
            var5.put("name", var3.toString());
            var1.put(var5);
         }
      }

      return var1;
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"resultKey", "direction", "sampleType", "charts"});
      String var2 = this.tryGetParamValue(var1, "charts");
      String[] var3 = var2.split(",");
      JSONObject var4 = new JSONObject();
      ResultsGroup var5 = null;

      try {
         if (this.rg == null) {
            var5 = rgProvider.get(var1, "TradeAnalysisServlet");
         } else {
            var5 = this.rg;
         }

         String var6 = ((String[])var1.get("resultKey"))[0].toString();
         byte var7 = Byte.parseByte(((String[])var1.get("direction"))[0]);
         byte var8 = Byte.parseByte(((String[])var1.get("sampleType"))[0]);
         OrdersList var9 = var5.orders().filterExcludingControlOrders(var6, var7, var8, true);
         JSONArray var10 = new JSONArray();

         for (int var11 = 0; var11 < var3.length; var11++) {
            JSONObject var12 = this.printChart(var1, var9, var3[var11]);
            var10.put(var12);
         }

         MainApp.settings().set("TradeAnalysisCharts", var2);
         var4.put("charts", var10);
         JSONObject var21 = this.printChart(var1, var9, "PLbyYearChart");
         var4.put("fixedChart", var21);
      } catch (RecordNotFoundException var17) {
         Log.info("onPrint: " + var17.getMessage());
         return apiErrorJSONNoLog(null, var17);
      } catch (Exception var18) {
         return apiErrorJSON(L.t("Getting trade analysis failed", new Object[0]), var18);
      } finally {
         if (var5 != null && this.rg == null) {
            var5.releaseLock("TradeAnalysisServlet");
            Object var20 = null;
            this.rg = null;
         }
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private JSONObject printChart(Map<String, String[]> var1, OrdersList var2, String var3) throws Exception {
      TradeAnalysisChart var4 = (TradeAnalysisChart)TradeAnalysisChartsList.getInstance().findClassByName(var3);
      byte var5 = Byte.parseByte(((String[])var1.get("period"))[0]);
      String var6 = ((String[])var1.get("year"))[0].toString();
      AbstractChart var7 = null;
      if (!var6.equals("All")) {
         int var8 = Integer.parseInt(var6);
         OrdersList var9 = new OrdersList("filteredByYear");

         for (int var10 = 0; var10 < var2.size(); var10++) {
            Order var11 = var2.get(var10);
            if (var8 == SQTime.getFullYear(var11.getTimeByPeriodType(var5))) {
               var9.add(var11);
            }
         }

         var7 = var4.draw(var9, (byte)10, var5);
         var9.clear();
      } else {
         var7 = var4.draw(var2, (byte)10, var5);
      }

      return var7.toJSON();
   }

   private String onGetAnnualStats(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "resultKey", "direction", "sampleType"});
      String var2 = ((String[])var1.get("resultKey"))[0].toString();
      byte var3 = Byte.parseByte(((String[])var1.get("direction"))[0]);
      byte var4 = Byte.parseByte(((String[])var1.get("sampleType"))[0]);
      byte var5 = Byte.parseByte(((String[])var1.get("period"))[0]);
      JSONObject var6 = new JSONObject();
      JSONArray var7 = new JSONArray();
      Object var8 = null;
      ResultsGroup var9 = null;

      try {
         var9 = rgProvider.get(var1, "TradeAnalysisServlet");
         var8 = var9.orders();
      } catch (RecordNotFoundException var21) {
         Log.info("onPrint: " + var21.getMessage());
         return apiErrorJSONNoLog(null, var21);
      } finally {
         if (var9 != null) {
            var9.releaseLock("TradeAnalysisServlet");
         }
      }

      if (var8 != null) {
         OrdersList var10 = var9.orders().filterExcludingControlOrders(var2, var3, var4, true);
         if (var10.size() > 0) {
            long var11 = -1L;
            long var13 = -1L;

            for (int var16 = 0; var16 < var10.size(); var16++) {
               Order var15 = var10.get(var16);
               if (var11 == -1L || var11 > var15.OpenTime) {
                  var11 = var15.OpenTime;
               }

               if (var13 == -1L || var13 < var15.CloseTime) {
                  var13 = var15.CloseTime;
               }
            }

            int var24 = SQTime.getFullYear(var11);
            int var17 = SQTime.getFullYear(var13);
            var7.put(this.calculateAnnualStats(0, var10, (byte)10, var5));

            for (int var18 = var17; var18 >= var24; var18--) {
               var7.put(this.calculateAnnualStats(var18, var10, (byte)10, var5));
            }
         }
      }

      var6.put("rows", var7);
      var6.put("success", "ok");
      return var6.toString();
   }

   private JSONObject calculateAnnualStats(int var1, OrdersList var2, byte var3, byte var4) {
      JSONObject var5 = new JSONObject();
      JSONArray var6 = this.statsComputer.compute(var1, var2, (byte)10, var4);
      var5.put("id", "r" + var1);
      var5.put("data", var6);
      return var5;
   }
}
