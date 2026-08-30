package com.strategyquant.plugin.Results.impl.OptimizationProfile;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.charts.Optimization2DChart;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.charts.Optimization3DChart;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.charts.OptimizationScatterChart;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.results.OptResults;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.results.OptResultsExport;
import com.strategyquant.plugin.Results.impl.OptimizationProfile.results.OptResultsViews;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.optimization.OptProfileChecksLevels;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationProfileServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(OptimizationProfileServlet.class);
   private static final String LOCK_OPTPROFSERVLET = "OptimizationProfileServlet";
   private IResultsGroupProvider rgProvider;
   private JSONArray columnValues = null;
   private Optimization3DChart optimization3d = null;
   private Optimization2DChart optimization2d = null;
   private OptimizationScatterChart optimizationScatter = null;
   private OptResultsViews resultsViews;
   private OptResults results;

   public OptimizationProfileServlet(IResultsGroupProvider var1) {
      this.rgProvider = var1;
      this.optimization3d = new Optimization3DChart();
      this.optimization2d = new Optimization2DChart();
      this.optimizationScatter = new OptimizationScatterChart();
      this.resultsViews = new OptResultsViews(MainApp.getDataPath() + "/user/settings/views/optimizationProfile");
      this.results = new OptResults();
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      if (var1.startsWith("views")) {
         return this.resultsViews.execute(var1, var2, var3);
      }

      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "printChart":
            return this.onPrintChart(var2);
         case "printResults":
            return this.onPrintResults(var2);
         case "getAvailableParams":
            return this.onGetAvailableParams(var2);
         case "export":
            return this.onExport(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      OptProfileChecksLevels var3 = new OptProfileChecksLevels();
      var3.evalProfitOptCheck = Boolean.parseBoolean(((String[])var1.get("evalProfitOptCheck"))[0]);
      var3.evalAvgProfitCheck = Boolean.parseBoolean(((String[])var1.get("evalAvgProfitCheck"))[0]);
      var3.evalUniformDistrCheck = Boolean.parseBoolean(((String[])var1.get("evalUniformDistrCheck"))[0]);
      var3.evalTopProfitCheck = Boolean.parseBoolean(((String[])var1.get("evalTopProfitCheck"))[0]);
      var3.profitableOptPct = Integer.parseInt(((String[])var1.get("profitOptPct"))[0]);
      var3.avgProfit = Double.parseDouble(((String[])var1.get("avgProfit"))[0]);
      var3.uniformDistrChanges = Integer.parseInt(((String[])var1.get("uniformDistrChanges"))[0]);
      var3.stdevAvgProfit = Double.parseDouble(((String[])var1.get("stdevAvgProfit"))[0]);
      ResultsGroup var4 = this.rgProvider.get(var1, "OptimizationProfileServlet");

      try {
         OptimizationProfile var5 = var4.getOptimizationProfile();
         var2.put("simulations", var5.printProfitableOptimizations());
         var2.put("checks", var5.printChecksResults(var3));
         var2.put("chart", var5.printPerformanceDistributionChart());
         var4.specialValues().set("OptProfileChecksLevels", var3);
      } finally {
         if (var4 != null) {
            var4.releaseLock("OptimizationProfileServlet");
            Object var9 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private String onGetAvailableParams(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = null;

      try {
         var3 = this.rgProvider.get(var1, "OptimizationProfileServlet");
         OptimizationProfile var4 = var3.getOptimizationProfile();
         if (var4 == null) {
            throw new Exception(L.t("No optimization results found.", new Object[0]));
         }

         var2.put("parameters", var4.getAvailableParams());
         var2.put("values", this.getColumnValues());
         OptProfileChecksLevels var5 = (OptProfileChecksLevels)var3.specialValues().get("OptProfileChecksLevels");
         if (var5 != null) {
            JSONObject var6 = new JSONObject();
            var6.put("evalProfitOptCheck", var5.evalProfitOptCheck);
            var6.put("evalAvgProfitCheck", var5.evalAvgProfitCheck);
            var6.put("evalUniformDistrCheck", var5.evalUniformDistrCheck);
            var6.put("evalTopProfitCheck", var5.evalTopProfitCheck);
            var6.put("profitOptPct", var5.profitableOptPct);
            var6.put("avgProfit", var5.avgProfit);
            var6.put("uniformDistrChanges", var5.uniformDistrChanges);
            var6.put("stdevAvgProfit", var5.stdevAvgProfit);
            var2.put("checks", var6);
         }
      } catch (RecordNotFoundException var11) {
         Log.info("onGetAvailableParams: " + var11.getMessage());
         return apiErrorJSONNoLog(null, var11);
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Getting available params failed", new Object[0]), var12);
      } finally {
         if (var3 != null) {
            var3.releaseLock("OptimizationProfileServlet");
            Object var14 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private synchronized JSONArray getColumnValues() {
      if (this.columnValues == null) {
         this.columnValues = new JSONArray();

         for (DatabankColumn var3 : DatabankColumns.get().getAvailableClasses()) {
            JSONObject var4 = new JSONObject();
            var4.put("name", var3.getName());
            var4.put("type", var3.getClass().getSimpleName());
            this.columnValues.put(var4);
         }
      }

      return this.columnValues;
   }

   private String onPrintChart(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      ResultsGroup var3 = this.rgProvider.get(var1, "OptimizationProfileServlet");

      try {
         this.checkParamExists(var1, new String[]{"xaxis", "yaxis", "zaxis"});
         String var4 = this.tryGetParamValue(var1, "xaxis");
         String var5 = this.tryGetParamValue(var1, "yaxis");
         String var6 = this.tryGetParamValue(var1, "zaxis");
         if (!this.optimization3d.has3DData(var3)) {
            var2.put("success", "ok");
            var2.put("nochart", "ok");
            return var2.toString();
         }

         String var7 = this.tryGetParamValue(var1, "type");
         String var8 = this.tryGetParamValue(var1, "chart");
         Object var9 = null;
         if (var8.equals("3D")) {
            var9 = this.optimization3d.print(var3, var4, var5, var6, var7);
         } else if (var8.equals("2D")) {
            var9 = this.optimization2d.print(var3, var4, var6);
         } else {
            if (!var8.equals("Scatter")) {
               throw new Exception(L.t("Invalid chart type. Must be one of: 3D, 2D, Scatter", new Object[0]));
            }

            var9 = this.optimizationScatter.print(var3, var6);
         }

         var2.put("chart", var9);
      } finally {
         if (var3 != null) {
            var3.releaseLock("OptimizationProfileServlet");
            Object var13 = null;
         }
      }

      var2.put("success", "ok");
      return var2.toString();
   }

   private String onPrintResults(Map<String, String[]> var1) throws Exception {
      ResultsGroup var2 = this.rgProvider.get(var1, "OptimizationProfileServlet");

      try {
         this.checkParamExists(var1, new String[]{"xaxis", "yaxis", "zaxis"});
         String var3 = this.tryGetParamValue(var1, "xaxis");
         String var4 = this.tryGetParamValue(var1, "yaxis");
         String var5 = this.tryGetParamValue(var1, "zaxis");
         String var6 = this.tryGetParamValue(var1, "type");
         String var7 = this.tryGetParamValue(var1, "chart");
         JSONArray var8 = new JSONArray();
         if (this.optimization3d.has3DData(var2)) {
            String var9 = ((String[])var1.get("view"))[0].toString();
            DatabankTableView var10 = this.resultsViews.getViewByName(var9);
            MainApp.settings().set("optProfileView", var9);
            var8 = this.results.print(var2, var3, var4, var5, var6, var7, var10);
         }

         JSONObject var15 = new JSONObject();
         var15.put("rows", var8);
         var15.put("success", "ok");
         return var15.toString();
      } finally {
         if (var2 != null) {
            var2.releaseLock("OptimizationProfileServlet");
            Object var14 = null;
         }
      }
   }

   private String onExport(Map<String, String[]> var1) throws Exception {
      ResultsGroup var2 = this.rgProvider.get(var1, "OptimizationProfileServlet");

      try {
         this.checkParamExists(var1, new String[]{"xaxis", "yaxis", "zaxis"});
         String var3 = this.tryGetParamValue(var1, "xaxis");
         String var4 = this.tryGetParamValue(var1, "yaxis");
         String var5 = this.tryGetParamValue(var1, "zaxis");
         String var6 = this.tryGetParamValue(var1, "type");
         String var7 = this.tryGetParamValue(var1, "chart");
         String var8 = ((String[])var1.get("path"))[0];
         boolean var9 = Boolean.parseBoolean(((String[])var1.get("useComma"))[0]);
         if (this.optimization3d.has3DData(var2)) {
            String var10 = ((String[])var1.get("view"))[0].toString();
            DatabankTableView var11 = this.resultsViews.getViewByName(var10);
            OptResultsExport var12 = new OptResultsExport();
            if (var8.endsWith("xlsx")) {
               var12.toXlsx(var8, var9, var2, var3, var4, var5, var6, var7, var11);
            } else {
               var12.toCsv(var8, var9, var2, var3, var4, var5, var6, var7, var11);
            }

            JSONObject var17 = new JSONObject();
            var17.put("success", "Results exported.");
            return var17.toString();
         } else {
            throw new Exception();
         }
      } finally {
         if (var2 != null) {
            var2.releaseLock("OptimizationProfileServlet");
            Object var16 = null;
         }
      }
   }
}
