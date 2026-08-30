package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysParamPermutationsTable {
   private static final Logger Log = LoggerFactory.getLogger(SysParamPermutationsTable.class);
   private Int2DoubleOpenHashMap mapMedians = new Int2DoubleOpenHashMap();
   private Int2DoubleOpenHashMap mapOrigValues = new Int2DoubleOpenHashMap();

   public SysParamPermutationsTable(OptimizationProfile var1) {
      List var2 = DatabankColumns.get().getAvailableClasses();
      ObjectArrayList var3 = var1.getOptimizations();
      OptimizationTestResult var4 = var1.getOriginalResult();

      for (int var5 = 0; var5 < var2.size(); var5++) {
         DatabankColumn var6 = (DatabankColumn)var2.get(var5);
         String var7 = var6.getClass().getSimpleName();

         try {
            double var8 = this.getMedian(var6, var3);
            double var10 = var4 == null ? 0.0 : var6.getNumericValue(var4.stats);
            int var12 = SQUtils.betterHashCode(var7);
            this.mapMedians.put(var12, var8);
            this.mapOrigValues.put(var12, var10);
         } catch (Exception var13) {
            Log.info("Error computing Sys Param Profile table", var13);
         }
      }
   }

   public SysParamPermutationsTable(ObjectInput var1) throws IOException {
      int var2 = var1.readInt();

      for (int var3 = 0; var3 < var2; var3++) {
         int var4 = var1.readInt();
         double var5 = var1.readDouble();
         this.mapMedians.put(var4, var5);
      }

      var2 = var1.readInt();

      for (int var8 = 0; var8 < var2; var8++) {
         int var9 = var1.readInt();
         double var10 = var1.readDouble();
         this.mapOrigValues.put(var9, var10);
      }
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeInt(this.mapMedians.size());
      ObjectIterator var2 = this.mapMedians.int2DoubleEntrySet().iterator();

      while (var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         var1.writeInt(var3.getIntKey());
         var1.writeDouble(var3.getDoubleValue());
      }

      var1.writeInt(this.mapOrigValues.size());
      var2 = this.mapOrigValues.int2DoubleEntrySet().iterator();

      while (var2.hasNext()) {
         Entry var5 = (Entry)var2.next();
         var1.writeInt(var5.getIntKey());
         var1.writeDouble(var5.getDoubleValue());
      }
   }

   private double getMedian(DatabankColumn var1, ObjectArrayList<OptimizationTestResult> var2) throws Exception {
      DoubleArrayList var3 = new DoubleArrayList();

      for (int var4 = 0; var4 < var2.size(); var4++) {
         OptimizationTestResult var5 = (OptimizationTestResult)var2.get(var4);
         double var6 = var1.getNumericValue(var5.stats);
         var3.add(var6);
      }

      Collections.sort(var3);
      return var3.size() == 0 ? 0.0 : var3.getDouble(var3.size() / 2);
   }

   public double getMedian(String var1) {
      int var2 = SQUtils.betterHashCode(var1);
      return this.mapMedians.containsKey(var2) ? this.mapMedians.get(var2) : 0.0;
   }

   public double getOrigValue(String var1) {
      int var2 = SQUtils.betterHashCode(var1);
      return this.mapOrigValues.containsKey(var2) ? this.mapOrigValues.get(var2) : 0.0;
   }
}
