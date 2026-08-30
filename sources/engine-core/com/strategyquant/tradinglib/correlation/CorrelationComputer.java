package com.strategyquant.tradinglib.correlation;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationComputer {
   public static final Logger Log = LoggerFactory.getLogger("CorrelationComputer");
   public static final int ERROR_VALUE = -99;
   public TimePeriod range;
   public String[] periodsStr;
   public double[] values1;
   public double[] values2;

   public double computeCorrelation(ResultsGroup var1, ResultsGroup var2, byte var3, int var4, CorrelationType var5, boolean var6) {
      try {
         OrdersList var7 = var3 == 127 ? var1.orders() : var1.orders().filter("Portfolio", (byte)0, var3);
         OrdersList var8 = var3 == 127 ? var2.orders() : var2.orders().filter("Portfolio", (byte)0, var3);
         CorrelationPeriods var9 = this.precomputePeriods(var1.getName(), var2.getName(), var7, var8, var4, var5);
         double var10 = this.computeCorrelation(var6, var1.getName(), var2.getName(), var7, var8, var9, var5);
         return SQUtils.round2(var10);
      } catch (Exception var12) {
         Log.error("Exc.", var12);
         return -99.0;
      }
   }

   public double computeCorrelation(boolean var1, String var2, String var3, OrdersList var4, OrdersList var5, CorrelationPeriods var6, CorrelationType var7) {
      try {
         if (var4 != null && var5 != null && var4.size() != 0 && var5.size() != 0) {
            DoubleArrayList var8 = new DoubleArrayList();
            DoubleArrayList var9 = new DoubleArrayList();
            TimePeriods var10 = (TimePeriods)var6.get(var2);
            TimePeriods var11 = (TimePeriods)var6.get(var3);
            ObjectBidirectionalIterator var14 = var10.long2ObjectEntrySet().iterator();

            while (var14.hasNext()) {
               Entry var15 = (Entry)var14.next();
               TimePeriod var12 = (TimePeriod)var10.get(var15.getLongKey());
               TimePeriod var13 = (TimePeriod)var11.get(var15.getLongKey());
               if ((var1 || var12.value != 0.0 || var13.value != 0.0) && !var7.shouldSkipPeriod(var12.value, var13.value)) {
                  var8.add(var12.value);
                  var9.add(var13.value);
               }
            }

            this.values1 = var8.toDoubleArray();
            this.values2 = var9.toDoubleArray();
            return calculateCorrelation(this.values1, this.values2);
         } else {
            return -99.0;
         }
      } catch (Exception var16) {
         Log.error("Exc.", var16);
         return -99.0;
      }
   }

   public static double calculateCorrelation(double[] var0, double[] var1) {
      double var6 = 0.0;
      double var8 = 0.0;
      double var10 = 0.0;
      double var12 = 0.0;
      double var14 = 0.0;
      double var16 = 0.0;
      double var18 = 0.0;
      double var20 = 0.0;
      double var22 = 0.0;

      for (int var24 = 0; var24 < var0.length; var24++) {
         double var2 = var0[var24];
         double var4 = var1[var24];
         var6 += var2 * var4;
         var8 += var2;
         var10 += var2 * var2;
         var12 += var4;
         var14 += var4 * var4;
         var16++;
      }

      if (var16 > 0.0) {
         var18 = var6 - var8 * var12 / var16;
         var20 = var10 - var8 * var8 / var16;
         var22 = var14 - var12 * var12 / var16;
      }

      return var0.length != 0 && var20 != 0.0 && var22 != 0.0 && var16 != 0.0 ? var18 / Math.sqrt(var20 * var22) : 1.0;
   }

   public static double calculateSimilarity(double[] var0, double[] var1) {
      double var2 = 0.0;
      double var4 = Double.MAX_VALUE;
      double var6 = -Double.MAX_VALUE;

      for (int var8 = 0; var8 < var0.length; var8++) {
         if (var0[var8] < var4) {
            var4 = var0[var8];
         }

         if (var1[var8] < var4) {
            var4 = var1[var8];
         }

         if (var0[var8] > var6) {
            var6 = var0[var8];
         }

         if (var1[var8] > var6) {
            var6 = var1[var8];
         }
      }

      double var13 = var6 - var4;

      for (int var10 = 0; var10 < var0.length; var10++) {
         double var11 = Math.abs(var0[var10] - var1[var10]);
         var11 /= var13;
         var2 += var11;
      }

      return 1.0 - var2 / var0.length;
   }

   public CorrelationPeriods precomputePeriods(String var1, String var2, OrdersList var3, OrdersList var4, int var5, CorrelationType var6) throws Exception {
      CorrelationPeriods var7 = new CorrelationPeriods();
      TimePeriod var8 = CorrelationLib.getPeriod(var3);
      long var9 = var8.from;
      long var11 = var8.to;
      var8 = CorrelationLib.getPeriod(var4);
      if (var8.from < var9) {
         var9 = var8.from;
      }

      if (var8.to > var11) {
         var11 = var8.to;
      }

      TimePeriods var13 = CorrelationLib.generatePeriods(var5, var9, var11);
      TimePeriods var14 = var13.clone();
      var6.computePeriods(var3, var5, var13);
      var7.put(var1, var13);
      var6.computePeriods(var4, var5, var14);
      var7.put(var2, var14);
      return var7;
   }
}
