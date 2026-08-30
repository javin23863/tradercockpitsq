package SQ.Functions;

import com.strategyquant.lib.SQUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class StatFunctions {
   public static double computeAverage(DoubleArrayList var0) {
      return computeAverage(var0, 0, -1);
   }

   public static double computeAverage(DoubleArrayList var0, int var1, int var2) {
      if (var0 != null && var0.size() != 0) {
         double var3 = 0.0;
         double var5 = 0.0;
         if (var2 == -1) {
            var2 = var0.size();
         }

         for (int var7 = var1; var7 < var2; var7++) {
            var5 = var0.getDouble(var7);
            var3 += var5;
         }

         return SQUtils.safeDivide(var3, var2 - var1);
      } else {
         return 0.0;
      }
   }

   public static double computeStdev(double var0, DoubleArrayList var2) {
      return computeStdev(var0, var2, 0, -1);
   }

   public static double computeStdev(double var0, DoubleArrayList var2, int var3, int var4) {
      if (var2 != null && var2.size() != 0) {
         if (var4 == -1) {
            var4 = var2.size();
         }

         double var5 = 0.0;

         for (int var11 = var3; var11 < var4; var11++) {
            double var9 = var2.getDouble(var11);
            var5 += Math.pow(var9 - var0, 2.0);
         }

         return Math.sqrt(var5 / (var4 - var3));
      } else {
         return 0.0;
      }
   }
}
