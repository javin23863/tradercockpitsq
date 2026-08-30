package com.strategyquant.tradinglib.strategy;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.results.IResultPlugin;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataItemsGenerator {
   private static final Logger Log = LoggerFactory.getLogger(DataItemsGenerator.class);

   public static void get(JSONObject var0, ResultsGroup var1) throws Exception {
      List var2 = var1.getResultKeys();
      if (var2 == null) {
         throw new Exception("Cannot get ResultsGroup keys");
      }

      WalkForwardMatrixResult var3 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
      JSONArray var4 = new JSONArray();

      for (String var6 : var2) {
         if (!var6.startsWith("WF:") || var3 == null) {
            var4.put(var6);
         }
      }

      if (var3 != null) {
         if (var3.isMatrix()) {
            for (int var10 = var3.start2; var10 <= var3.stop2; var10 += var3.increment2) {
               for (int var13 = var3.start1; var13 <= var3.stop1; var13 += var3.increment1) {
                  WalkForwardResult var7 = var3.getWFResult(var13, var10);
                  var4.put(var7.getResultKeyName());
               }
            }
         } else {
            WalkForwardResult var11 = var3.getResultList().get(0);
            var4.put(var11.getResultKeyName());
         }
      }

      var0.put("dataItems", var4);
      JSONArray var12 = new JSONArray();

      for (IResultPlugin var15 : SQPluginManager.getPlugins(IResultPlugin.class)) {
         try {
            if (var15.containsResult(var1)) {
               continue;
            }
         } catch (Exception var9) {
            Log.error("Error while checking if strategy contains result '" + var15.getClass().getSimpleName() + "'. Exc.", var9);
         }

         var12.put(var15.getKey());
      }

      var0.put("noResultItems", var12);
      var0.put("isPortfolio", var1.isPortfolio());
      var0.put("resultInfo", getResultInfo(var1));
      var0.put("isStockpicker", var1.isStockpickerStrategy());
      var0.put("resultTypes", getResultTypes(var1));
   }

   private static JSONArray getResultTypes(ResultsGroup var0) throws Exception {
      JSONArray var1 = new JSONArray();
      byte[] var2 = SampleTypes.list();

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var0.mainResult().statsOrNull((byte)0, (byte)10, var2[var3]) != null) {
            var1.put(var2[var3]);
         }
      }

      return var1;
   }

   public static String getResultInfo(ResultsGroup var0) {
      try {
         SettingsMap var1 = var0.specialValues();
         String var2 = var0.mainResult().getString(SpecialValues.Symbol, "N/A");
         String var3 = var0.mainResult().getString(SpecialValues.Timeframe, "N/A");
         String var4 = SQTime.toString(var1.getLong(SpecialValues.HistoryFrom), "yyyy.MM.dd");
         String var5 = SQTime.toString(var1.getLong(SpecialValues.HistoryTo), "yyyy.MM.dd");
         return var2 + ", " + var3 + ", " + var4 + " - " + var5;
      } catch (Exception var6) {
         return "N/A";
      }
   }
}
