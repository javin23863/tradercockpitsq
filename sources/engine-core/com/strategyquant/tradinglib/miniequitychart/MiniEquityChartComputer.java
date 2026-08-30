package com.strategyquant.tradinglib.miniequitychart;

import com.strategyquant.lib.TimePeriod;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.equitychart.ChartOptimalizer;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.equitychart.Periods;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniEquityChartComputer {
   private static final Logger Log = LoggerFactory.getLogger("MiniEquityChartComputer");
   private static final String KEY_ST_FULL = "MEC_FULL_";
   private static final String KEY_ST_IS = "MEC_IS_";
   private static final String KEY_ST_OOS = "MEC_OOS_";

   public static String getValue(ResultsGroup var0, String var1, byte var2) throws Exception {
      return getValue(var0, var1, var2, 50);
   }

   public static String getValue(ResultsGroup var0, String var1, byte var2, int var3) throws Exception {
      String var4 = getSpecialValuesKey(var0, var1, var2);
      if (var0.specialValues().containsKey(var4)) {
         return var0.specialValues().getString(var4);
      }

      String var5 = computeMiniEquity(var0, var1, var2, var3);
      var0.specialValues().setString(var4, var5);
      var0.updated = true;
      return var5;
   }

   public static void computeForResult(ResultsGroup var0, String var1) throws Exception {
      String var3 = getSpecialValuesKey(var0, var1, (byte)127);
      if (!var0.specialValues().containsKey(var3)) {
         String var2 = computeMiniEquity(var0, var1, (byte)127);
         var0.specialValues().setString(var3, var2);
      }

      var3 = getSpecialValuesKey(var0, var1, (byte)10);
      if (!var0.specialValues().containsKey(var3)) {
         String var4 = computeMiniEquity(var0, var1, (byte)10);
         var0.specialValues().setString(var3, var4);
      }

      var3 = getSpecialValuesKey(var0, var1, (byte)20);
      if (!var0.specialValues().containsKey(var3)) {
         String var5 = computeMiniEquity(var0, var1, (byte)20);
         var0.specialValues().setString(var3, var5);
      }
   }

   public static String getSpecialValuesKey(ResultsGroup var0, String var1, byte var2) {
      StringBuilder var3 = new StringBuilder();
      var1 = getResultKey(var0, var1);
      var1 = correctResultKey(var1);
      if (var2 == 10) {
         var3.append("MEC_IS_");
      } else if (var2 == 20) {
         var3.append("MEC_OOS_");
      } else {
         var3.append("MEC_FULL_");
      }

      var3.append(var1);
      return var3.toString();
   }

   public static String computeMiniEquity(ResultsGroup var0, String var1, byte var2) {
      return computeMiniEquity(var0, var1, var2, 50);
   }

   public static String computeMiniEquity(ResultsGroup var0, String var1, byte var2, int var3) {
      try {
         OrdersList var4 = var0.orders().filter(var1, (byte)0, var2, true);
         MainChartDataset var5 = EquityChartUtils.generateTradeDataset(var4);
         ChartOptimalizer.OptimizerResult var6 = new ChartOptimalizer()
            .optimalize(false, var5.xVals, var5.yVals, 0L, var5.xVals.length - 1L, var3 < 0 ? var4.size() : var3);
         int[] var7 = new int[var6.yVals.length];
         double var8 = Double.MAX_VALUE;
         double var10 = -Double.MAX_VALUE;
         byte var12 = 17;

         for (int var13 = 0; var13 < var6.size(); var13++) {
            if (var6.yVals[var13] < var8) {
               var8 = var6.yVals[var13];
            }

            if (var6.yVals[var13] > var10) {
               var10 = var6.yVals[var13];
            }
         }

         for (int var23 = 0; var23 < var6.size(); var23++) {
            var7[var23] = map(var6.yVals[var23], var8, var10, 0.0, var12);
         }

         byte var24 = 0;
         if (var8 < 0.0) {
            var24 = (byte)(var7.length > 0 ? map(0.0, var8, var10, 0.0, var12) : 0);
         }

         JSONObject var14 = new JSONObject();
         JSONArray var15 = new JSONArray();

         for (int var16 = 0; var16 < var7.length; var16++) {
            var15.put(var7[var16]);
         }

         var14.put("values", var15);
         if (var2 == 127 || var2 == 10) {
            Periods var25 = new Periods();
            List var17 = var25.getAvailablePeriods(var4, "trade");
            int[][] var18 = var2 == 127 ? getSampleMarkersByType(var25.filter(var17, (byte)20), var4, var6) : null;
            int[][] var19 = getSampleMarkersByType(var25.filter(var17, (byte)40), var4, var6);
            if (var18 != null && var18.length > 0) {
               JSONArray var20 = new JSONArray();

               for (int var21 = 0; var21 < var18.length; var21++) {
                  var20.put(new JSONArray().put(var18[var21][0]).put(var18[var21][1]));
               }

               var14.put("oos", var20);
            }

            if (var19 != null && var19.length > 0) {
               JSONArray var26 = new JSONArray();

               for (int var27 = 0; var27 < var19.length; var27++) {
                  var26.put(new JSONArray().put(var19[var27][0]).put(var19[var27][1]));
               }

               var14.put("isv", var26);
            }
         }

         var14.put("zeroPoint", var24);
         return "{{sparklinesWidget data='" + var14.toString() + "'}}";
      } catch (Exception var22) {
         Log.error("Cannot compute mini equity chart", var22);
         return "";
      }
   }

   public static String correctResultKey(String var0) {
      return var0.replaceAll("[^\\dA-Za-z ]", "__").replaceAll("\\s+", "_");
   }

   private static String getResultKey(ResultsGroup var0, String var1) {
      try {
         if (var1.equals(var0.getMainResultKey()) || var1.equals("Portfolio") && var0.mainResult().get("WalkForwardResult") != null) {
            return "Main";
         }
      } catch (Exception var3) {
         Log.error("Cannot correct resultkey", var3);
      }

      return var1;
   }

   private static int[][] getSampleMarkersByType(List<TimePeriod> var0, OrdersList var1, ChartOptimalizer.OptimizerResult var2) throws Exception {
      int[][] var3 = null;
      if (var0.size() > 0) {
         var3 = new int[var0.size()][2];

         for (int var4 = 0; var4 < var0.size(); var4++) {
            TimePeriod var5 = (TimePeriod)var0.get(var4);
            var3[var4][0] = -1;
            var3[var4][1] = -1;

            for (int var6 = 0; var6 < var2.size(); var6++) {
               if (var3[var4][0] == -1 && var5.from <= var2.xVals[var6]) {
                  var3[var4][0] = var6;
               }

               if (var3[var4][1] == -1 && var5.to <= var2.xVals[var6]) {
                  var3[var4][1] = var6;
               }
            }
         }

         if (var3[var0.size() - 1][1] == -1) {
            var3[var0.size() - 1][1] = var2.size() - 1;
         }
      }

      return var3;
   }

   public static int map(double var0, double var2, double var4, double var6, double var8) {
      return new Double((var0 - var2) * (var8 - var6) / (var4 - var2) + var6).intValue();
   }
}
