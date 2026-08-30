package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysParamPermutationsCharts {
   private static final Logger Log = LoggerFactory.getLogger(SysParamPermutationsCharts.class);
   private Int2ObjectOpenHashMap<JSONObject> mapCharts = new Int2ObjectOpenHashMap();

   public SysParamPermutationsCharts(OptimizationProfile var1) {
      List var2 = DatabankColumns.get().getAvailableClasses();
      ObjectArrayList var3 = var1.getOptimizations();
      OptimizationTestResult var4 = var1.getOriginalResult();

      for (int var5 = 0; var5 < var2.size(); var5++) {
         DatabankColumn var6 = (DatabankColumn)var2.get(var5);
         String var7 = var6.getClass().getSimpleName();

         try {
            JSONObject var8 = this.computeFor(var7, var3, var4);
            int var9 = SQUtils.betterHashCode(var7);
            this.mapCharts.put(var9, var8);
         } catch (Exception var10) {
            Log.info("Error computing Sys Param Profile chart", var10);
         }
      }
   }

   public SysParamPermutationsCharts(ObjectInput var1) throws IOException {
      int var2 = var1.readInt();

      for (int var3 = 0; var3 < var2; var3++) {
         int var4 = var1.readInt();
         String var5 = var1.readUTF();
         this.mapCharts.put(var4, new JSONObject(var5));
      }
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeInt(this.mapCharts.size());
      IntSet var2 = this.mapCharts.keySet();
      IntIterator var3 = var2.iterator();

      while (var3.hasNext()) {
         int var4 = (Integer)var3.next();
         var1.writeInt(var4);
         var1.writeUTF(((JSONObject)this.mapCharts.get(var4)).toString());
      }
   }

   public JSONObject get(String var1) {
      int var2 = SQUtils.betterHashCode(var1);
      return this.mapCharts.containsKey(var2) ? (JSONObject)this.mapCharts.get(var2) : null;
   }

   public JSONObject computeFor(String var1, ObjectArrayList<OptimizationTestResult> var2, OptimizationTestResult var3) throws Exception {
      JSONObject var4 = new JSONObject();
      DatabankColumn var5 = (DatabankColumn)DatabankColumns.get().findClassByName(var1);
      double var6 = Double.MAX_VALUE;
      double var8 = -Double.MAX_VALUE;
      DoubleArrayList var10 = new DoubleArrayList();

      for (int var11 = 0; var11 < var2.size(); var11++) {
         OptimizationTestResult var12 = (OptimizationTestResult)var2.get(var11);
         double var13 = var12.stats.getDouble(var1);
         if (var13 > var8) {
            var8 = var13;
         }

         if (var13 < var6) {
            var6 = var13;
         }

         var10.add(var13);
      }

      int var26 = var10.size();
      if (var26 > 20) {
         var26 = 20;
      }

      double var27 = (var8 - var6) / (var26 - 1);
      int[] var14 = new int[var26];
      double[] var15 = new double[var26];

      for (int var16 = 0; var16 < var15.length; var16++) {
         var15[var16] = -99999.0;
      }

      if (var10.size() == 0) {
         return var4;
      }

      for (int var28 = 0; var28 < var10.size(); var28++) {
         double var17 = var10.getDouble(var28);
         int var19 = this.getHistogramIndex(var17, var6, var8, var27, var26);
         if (var19 < 0 || var19 >= var14.length) {
            var19 = this.getHistogramIndex(var17, var6, var8, var27, var26);
         }

         var14[var19]++;
         if (var15[var19] == -99999.0 || var15[var19] < var17) {
            var15[var19] = var17;
         }
      }

      int var29 = 0;
      BarChart var33 = new BarChart();
      var33.tooltips = false;

      for (int var20 = 0; var20 < var14.length; var20++) {
         int var30 = var14[var20];
         String var18 = SQUtils.d2(var15[var20]);
         if (var30 > var29) {
            var29 = var30;
         }

         var33.addValue("Frequency", var18, var30);
      }

      Collections.sort(var10);
      double var34 = var10.getDouble(var10.size() / 2);
      var4.put("median", var5.printPlValue(var34, (byte)10));
      var33.setCategoryColor("Median", "#E8383C");
      int var22 = this.getHistogramIndex(var34, var6, var8, var27, var26);

      for (int var23 = 0; var23 < var14.length; var23++) {
         String var31;
         if (var15[var23] == -99999.0) {
            var31 = "";
         } else {
            var31 = SQUtils.d2(var15[var23]);
         }

         if (var23 == var22) {
            var33.addValue("Median", var31, var29);
         } else {
            var33.addValue("Median", var31, 0);
         }
      }

      if (var3 != null) {
         double var36 = var5.getNumericValue(var3.stats);
         var33.setCategoryColor("Orig. value", "#008000");
         var22 = this.getHistogramIndex(var36, var6, var8, var27, var26);

         for (int var25 = 0; var25 < var14.length; var25++) {
            String var32;
            if (var15[var25] == -99999.0) {
               var32 = "";
            } else {
               var32 = SQUtils.d2(var15[var25]);
            }

            if (var25 == var22) {
               var33.addValue("Orig. value", var32, var29);
            } else {
               var33.addValue("Orig. value", var32, 0);
            }
         }
      }

      var4.put("chart", var33.toJSON());
      return var4;
   }

   private int getHistogramIndex(double var1, double var3, double var5, double var7, int var9) {
      double var10 = var1 - var3;
      var10 /= var7;
      return (int)var10;
   }
}
