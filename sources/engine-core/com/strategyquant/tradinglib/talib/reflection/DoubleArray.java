package com.strategyquant.tradinglib.talib.reflection;

import com.strategyquant.tradinglib.engine.stockpicker.data.BarData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;

public class DoubleArray {
   private ArrayList<double[]> arrays = new ArrayList<>();
   private Integer[] columns;

   public DoubleArray(ObjectArrayList<BarData> var1, Integer... var2) {
      this.columns = var2;
      int var3 = var1.size();

      for (int var4 = 0; var4 < var2.length; var4++) {
         this.arrays.add(new double[var3]);
      }
   }

   public double[] get(int var1) {
      int var2 = 0;
      Integer[] var3 = this.columns;
      int var4 = var3.length;

      for (int var5 = 0; var5 < var4; var5++) {
         int var6 = var3[var5];
         if (var6 == var1) {
            return this.arrays.get(var2);
         }

         var2++;
      }

      return null;
   }

   public double[] getByIndex(int var1) {
      return this.arrays.get(var1);
   }
}
