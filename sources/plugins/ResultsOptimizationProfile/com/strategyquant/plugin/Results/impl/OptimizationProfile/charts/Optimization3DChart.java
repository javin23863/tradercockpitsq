package com.strategyquant.plugin.Results.impl.OptimizationProfile.charts;

import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Optimization3DChart {
   private static final Logger Log = LoggerFactory.getLogger(Optimization3DChart.class);

   public JSONObject print(ResultsGroup var1, String var2, String var3, String var4, String var5) throws Exception {
      OptimizationProfile var6 = var1.getOptimizationProfile();
      ObjectArrayList var7 = var6.getOptimizations().clone();
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var4);
      JSONObject var9 = new JSONObject();
      JSONArray var10 = new JSONArray();
      DoubleArrayList var11 = new DoubleArrayList();
      DoubleArrayList var12 = new DoubleArrayList();
      String var19 = null;

      for (int var20 = 0; var20 < var7.size(); var20++) {
         OptimizationTestResult var21 = (OptimizationTestResult)var7.get(var20);
         Int2DoubleOpenHashMap var22 = var21.parseParams();
         if (var22.containsKey(var2.hashCode())) {
            double var13 = var22.get(var2.hashCode());
            if (!var11.contains(var13)) {
               var11.add(var13);
            }
         }

         if (var22.containsKey(var3.hashCode())) {
            double var15 = var22.get(var3.hashCode());
            if (!var12.contains(var15)) {
               var12.add(var15);
            }
         }
      }

      Collections.sort(var11);
      Collections.sort(var12);
      double var37 = -9.99999999E8;
      double var38 = Double.MAX_VALUE;
      double var24 = Double.MAX_VALUE;
      Optimization3DChart.Group[][] var26 = new Optimization3DChart.Group[var11.size()][var12.size()];

      for (int var28 = 0; var28 < var7.size(); var28++) {
         OptimizationTestResult var29 = (OptimizationTestResult)var7.get(var28);
         Int2DoubleOpenHashMap var30 = var29.parseParams();

         for (int var31 = 0; var31 < var11.size(); var31++) {
            for (int var32 = 0; var32 < var12.size(); var32++) {
               double var33 = var11.get(var31);
               double var35 = var12.get(var32);
               if (var30.containsKey(var2.hashCode())
                  && var30.get(var2.hashCode()) == var33
                  && var30.containsKey(var3.hashCode())
                  && var30.get(var3.hashCode()) == var35) {
                  double var17 = var8.getNumericValue(var29.stats);
                  if (var17 > var37) {
                     var37 = var17;
                     var19 = var1.getName();
                  }

                  if (var24 == Double.MAX_VALUE || var24 > var17) {
                     var24 = var17;
                  }

                  if (var38 == Double.MAX_VALUE || var38 < var17) {
                     var38 = var17;
                  }

                  if (var26[var31][var32] == null) {
                     var26[var31][var32] = new Optimization3DChart.Group();
                  }

                  var26[var31][var32].add(var17, var29.params);
               }
            }
         }
      }

      for (int var39 = 0; var39 < var11.size(); var39++) {
         for (int var40 = 0; var40 < var12.size(); var40++) {
            Optimization3DChart.Group var27 = var26[var39][var40];
            if (var27 != null) {
               double var34 = var11.get(var39);
               double var36 = var12.get(var40);
               if (!var5.equals("top") && !var5.equals("surface") && !var5.equals("dot-color-one")) {
                  for (int var43 = 0; var43 < var27.size(); var43++) {
                     var10.put(
                        new JSONObject()
                           .put("x", var34)
                           .put("y", var36)
                           .put("z", var27.zvalues[var43])
                           .put("style", var27.zvalues[var43])
                           .put("str", var1.getName())
                           .put("params", var27.params[var43])
                     );
                  }
               } else if (var8.getValueType() == 1) {
                  int var41 = var27.getMinIndex();
                  var10.put(
                     new JSONObject()
                        .put("x", var34)
                        .put("y", var36)
                        .put("z", var27.zvalues[var41])
                        .put("style", var27.zvalues[var41])
                        .put("str", var1.getName())
                        .put("params", var27.params[var41])
                  );
               } else {
                  int var42 = var27.getMaxIndex();
                  var10.put(
                     new JSONObject()
                        .put("x", var34)
                        .put("y", var36)
                        .put("z", var27.zvalues[var42])
                        .put("style", var27.zvalues[var42])
                        .put("str", var1.getName())
                        .put("params", var27.params[var42])
                  );
               }
            }
         }
      }

      var9.put("values", var10);
      var9.put("xlabel", var2);
      var9.put("ylabel", var3);
      var9.put("zlabel", var8.getName());
      var9.put("zmin", var24);
      var9.put("zmax", var38);
      var9.put("bestStrategyName", var19);
      var9.put("reverseColorScale", var8.getValueType() == 2);
      return var9;
   }

   public boolean has3DData(ResultsGroup var1) {
      OptimizationProfile var2 = var1.getOptimizationProfile();
      if (var2 == null) {
         return false;
      }

      ObjectArrayList var3 = var2.getOptimizations();
      return var3 != null && var3.size() != 0;
   }

   private class Group {
      private static final int COUNT = 5;
      public double[] zvalues = new double[5];
      public String[] params = new String[5];
      private int index = -1;

      private Group() {
      }

      public void add(Double var1, String var2) {
         if (this.index < 4) {
            this.index++;
            this.zvalues[this.index] = var1;
            this.params[this.index] = var2;
         } else {
            int var3 = 0;
            int var4 = 0;

            for (int var5 = 0; var5 < this.size(); var5++) {
               if (this.zvalues[var5] < this.zvalues[var3]) {
                  var3 = var5;
               }

               if (this.zvalues[var5] > this.zvalues[var4]) {
                  var4 = var5;
               }
            }

            if (var1 < this.zvalues[var3]) {
               this.zvalues[var3] = var1;
               this.params[var3] = var2;
            } else if (var1 > this.zvalues[var4]) {
               this.zvalues[var4] = var1;
               this.params[var4] = var2;
            }
         }
      }

      public int getMaxIndex() {
         int var1 = 0;

         for (int var2 = 0; var2 < this.size(); var2++) {
            if (this.zvalues[var2] > this.zvalues[var1]) {
               var1 = var2;
            }
         }

         return var1;
      }

      public int getMinIndex() {
         int var1 = 0;

         for (int var2 = 0; var2 < this.size(); var2++) {
            if (this.zvalues[var2] < this.zvalues[var1]) {
               var1 = var2;
            }
         }

         return var1;
      }

      public int size() {
         return this.index + 1;
      }
   }
}
