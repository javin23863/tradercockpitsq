package com.strategyquant.plugin.Results.impl.OptimizationProfile.results;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

public class OptResults {
   public JSONArray print(ResultsGroup var1, String var2, String var3, String var4, String var5, String var6, DatabankTableView var7) {
      OptimizationProfile var8 = var1.getOptimizationProfile();
      ObjectArrayList var9 = var8.getOptimizations().clone();
      Collections.sort(var9, new OptComparatorByFitness());
      JSONArray var10 = new JSONArray();

      for (int var11 = 0; var11 < var9.size(); var11++) {
         OptimizationTestResult var12 = (OptimizationTestResult)var9.get(var11);
         var10.put(this.printRow(var12, var7, var11, var6, var2, var3));
         if (var11 == 500) {
            break;
         }
      }

      return var10;
   }

   private JSONObject printRow(OptimizationTestResult var1, DatabankTableView var2, int var3, String var4, String var5, String var6) {
      JSONObject var7 = new JSONObject();
      JSONArray var8 = new JSONArray();
      ArrayList var9 = var2.columns;

      for (int var10 = 0; var10 < var9.size(); var10++) {
         DatabankColumn var11 = ((DatabankTableColumnEntry)var9.get(var10)).tableColumn;

         try {
            String var12 = var11.printValue(var1.stats);
            var8.put(var12);
         } catch (Exception var13) {
            var8.put("N/A");
         }
      }

      Int2DoubleOpenHashMap var14 = var1.parseParams();
      if (var4.equals("3D")) {
         if (var14.containsKey(var5.hashCode())) {
            double var15 = var14.get(var5.hashCode());
            var8.put(var15);
         } else {
            var8.put("N/A");
         }

         if (var14.containsKey(var6.hashCode())) {
            double var16 = var14.get(var6.hashCode());
            var8.put(var16);
         } else {
            var8.put("N/A");
         }
      } else if (var4.equals("2D")) {
         if (var14.containsKey(var5.hashCode())) {
            double var17 = var14.get(var5.hashCode());
            var8.put(var17);
         } else {
            var8.put("N/A");
         }
      }

      var7.put("id", "r" + var3);
      var7.put("data", var8);
      return var7;
   }
}
