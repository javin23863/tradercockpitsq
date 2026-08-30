package com.strategyquant.plugin.Results.impl.SysParamPermutation;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.SysParamPermutationsCharts;
import com.strategyquant.tradinglib.optimization.SysParamPermutationsTable;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysParamPermutationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SysParamPermutationServlet.class);
   private static final String LOCK_SYSPARAMPERMSERVLET = "SysParamPermutationServlet";
   private static IResultsGroupProvider rgProvider;

   public SysParamPermutationServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "printTable":
            return this.onPrintTable(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONObject loadLastSettings() throws Exception {
      JSONObject var1 = new JSONObject();
      String var2 = MainApp.settings().get("SPPCharts", "NetProfit,Drawdown,DrawdownPct,ReturnDDRatio,SharpeRatio,CAGR");
      var1.put("charts", SQUtils.toJSONArray(var2));
      String var3 = MainApp.settings().get("SPPStats", "NetProfit,Drawdown,DrawdownPct,ReturnDDRatio,SharpeRatio,CAGR");
      var1.put("stats", SQUtils.toJSONArray(var3));
      return var1;
   }

   private String onPrintTable(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"statsKeys"});
      String var2 = this.tryGetParamValue(var1, "statsKeys");
      String[] var3 = var2.split(",");
      JSONObject var4 = new JSONObject();
      ResultsGroup var5 = null;

      try {
         var5 = rgProvider.get(var1, "SysParamPermutationServlet");
         OptimizationProfile var6 = var5.getOptimizationProfile();
         if (var6 == null) {
            var4.put("stats", new JSONArray());
            var4.put("success", "ok");
            return var4.toString();
         }

         SysParamPermutationsTable var7 = var6.getSysParamPermutationsTable();
         JSONArray var8 = new JSONArray();

         for (int var9 = 0; var9 < var3.length; var9++) {
            String var10 = var3[var9];
            DatabankColumn var11 = (DatabankColumn)DatabankColumns.get().findClassByName(var10);
            double var12 = var7.getMedian(var10);
            double var14 = var7.getOrigValue(var10);
            double var16 = var14 / var12 * 100.0;
            JSONObject var18 = new JSONObject();
            var18.put("median", var11.printPlValue(var12, (byte)10));
            var18.put("orig", var11.printPlValue(var14, (byte)10));
            var18.put("origMedianPct", PlTypes.printPL(var16, (byte)20));
            var8.put(var18);
         }

         MainApp.settings().set("SPPStats", var2);
         var4.put("stats", var8);
      } catch (RecordNotFoundException var23) {
         Log.info("onPrintTable: " + var23.getMessage());
         return apiErrorJSONNoLog(null, var23);
      } catch (Exception var24) {
         Log.error("onPrintTable: " + var24.getMessage());
         return apiErrorJSONNoLog("SysParamPermutation error", var24);
      } finally {
         if (var5 != null) {
            var5.releaseLock("SysParamPermutationServlet");
            Object var26 = null;
         }
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"statsKeys"});
      String var2 = this.tryGetParamValue(var1, "statsKeys");
      String[] var3 = var2.split(",");
      JSONObject var4 = new JSONObject();
      ResultsGroup var5 = null;

      try {
         var5 = rgProvider.get(var1, "SysParamPermutationServlet");
         OptimizationProfile var6 = var5.getOptimizationProfile();
         if (var6 == null) {
            var4.put("charts", new JSONArray());
            var4.put("success", "ok");
            return var4.toString();
         }

         SysParamPermutationsCharts var7 = var6.getSysParamPermutationsCharts();
         JSONArray var8 = new JSONArray();

         for (int var9 = 0; var9 < var3.length; var9++) {
            JSONObject var10 = var7.get(var3[var9]);
            var8.put(var10);
         }

         MainApp.settings().set("SPPCharts", var2);
         var4.put("charts", var8);
      } catch (RecordNotFoundException var15) {
         Log.info("onPrint: " + var15.getMessage());
         return apiErrorJSONNoLog(null, var15);
      } catch (Exception var16) {
         Log.error("onPrint: " + var16.getMessage());
         return apiErrorJSONNoLog("SysParamPermutation error", var16);
      } finally {
         if (var5 != null) {
            var5.releaseLock("SysParamPermutationServlet");
            Object var18 = null;
         }
      }

      var4.put("success", "ok");
      return var4.toString();
   }
}
