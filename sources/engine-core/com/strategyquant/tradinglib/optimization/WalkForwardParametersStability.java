package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForwardParametersStability {
   private static final Logger Log = LoggerFactory.getLogger(WalkForwardParametersStability.class);
   private static final Comparator<Double> comp = Comparator.naturalOrder();

   public static void compute(ResultsGroup var0, OptimizationParams var1) {
      try {
         HashMap var2 = new HashMap();
         HashMap var3 = new HashMap();
         HashMap var4 = new HashMap();
         HashMap var5 = new HashMap();

         for (int var6 = 0; var6 < var1.size(); var6++) {
            Parameter var7 = var1.get(var6);
            var2.put(var7.getName(), var7.getStart());
            var3.put(var7.getName(), var7.getStop());
         }

         WalkForwardMatrixResult var17 = (WalkForwardMatrixResult)var0.mainResult().get("WalkForwardResult");
         if (var17 != null) {
            ArrayList var18 = var17.getResultList();

            for (int var8 = 0; var8 < var18.size(); var8++) {
               WalkForwardResult var9 = (WalkForwardResult)var18.get(var8);
               var4.clear();

               for (int var10 = 0; var10 < var9.wfPeriods.size(); var10++) {
                  WalkForwardPeriod var11 = var9.wfPeriods.get(var10);
                  loadParamValues(var11.testParameters, var4);
               }

               double var21 = calculateParametersStability(var2, var3, var4);
               String var12 = var9.getResultKeyName();
               var5.put(var12, var21);
               if (var8 == 0) {
                  var0.specialValues().set(getSpecialValuesKey(var0.getMainResultKey()), var21);
               }
            }

            double var19 = 0.0;
            double var22 = Double.MAX_VALUE;

            for (String var13 : var5.keySet()) {
               double var14 = (Double)var5.get(var13);
               var19 += var14;
               var22 = Math.min(var22, var14);
               var0.specialValues().set(getSpecialValuesKey(var13), var14);
            }

            var19 /= var5.size();
            var0.specialValues().set(SpecialValues.AvgParametersStability, SQUtils.round2(var19));
            var0.specialValues().set(SpecialValues.WorstParametersStability, SQUtils.round2(var22));
         }
      } catch (Exception var16) {
         Log.error("WF parameters stability calculation failed", var16);
      }
   }

   public static String getSpecialValuesKey(String var0) {
      var0 = var0.replaceAll("[^a-zA-Z0-9_]+", "_");
      return String.format("%s_%s", SpecialValues.ParametersStability, var0);
   }

   private static void loadParamValues(String var0, HashMap<String, ArrayList<Double>> var1) {
      if (var0 != null && !var0.isEmpty()) {
         String[] var2 = var0.split(",");

         for (int var3 = 0; var3 < var2.length; var3++) {
            String var4 = var2[var3];
            String[] var5 = var4.split("=");
            if (var5.length == 2) {
               String var6 = var5[0];
               String var7 = var5[1];

               try {
                  double var8 = Double.parseDouble(var7);
                  ArrayList var10 = (ArrayList)var1.get(var6);
                  if (var10 == null) {
                     var10 = new ArrayList();
                     var1.put(var6, var10);
                  }

                  var10.add(var8);
               } catch (Exception var11) {
               }
            }
         }
      }
   }

   private static double calculateParametersStability(HashMap<String, Double> var0, HashMap<String, Double> var1, HashMap<String, ArrayList<Double>> var2) {
      double var3 = 0.0;
      if (var2.size() == 0) {
         return -1.0;
      }

      for (String var6 : var2.keySet()) {
         ArrayList var7 = (ArrayList)var2.get(var6);
         if (var0.containsKey(var6)) {
            double var8 = (Double)var1.get(var6) - (Double)var0.get(var6);
            if (var8 != 0.0) {
               double var10 = median(var7);
               double var12 = 0.0;

               for (int var14 = 0; var14 < var7.size(); var14++) {
                  double var15 = (Double)var7.get(var14);
                  var12 += Math.abs(var15 - var10);
               }

               var12 /= var7.size();
               double var19 = 1.0 - var12 / var8;
               var3 += var19;
            }
         }
      }

      var3 /= var2.size();
      return SQUtils.round2(var3);
   }

   private static double median(ArrayList<Double> var0) {
      int var3 = var0.size() / 2;
      double var1;
      if (var0.size() % 2 == 0) {
         var1 = (nth(var0, var3 - 1) + nth(var0, var3)) / 2.0;
      } else {
         var1 = nth(var0, var3);
      }

      return var1;
   }

   private static double nth(ArrayList<Double> var0, int var1) {
      ArrayList var6 = new ArrayList();
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();
      double var4 = (Double)var0.get(var1 / 2);

      for (double var10 : var0) {
         int var12 = comp.compare(var10, var4);
         if (var12 < 0) {
            var6.add(var10);
         } else if (var12 > 0) {
            var7.add(var10);
         } else {
            var8.add(var10);
         }
      }

      double var2;
      if (var1 < var6.size()) {
         var2 = nth(var6, var1);
      } else if (var1 < var6.size() + var8.size()) {
         var2 = var4;
      } else {
         var2 = nth(var7, var1 - var6.size() - var8.size());
      }

      return var2;
   }
}
