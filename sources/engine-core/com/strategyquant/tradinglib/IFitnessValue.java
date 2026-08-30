package com.strategyquant.tradinglib;

public interface IFitnessValue {
   default double transformToFitnessRange(double var1, double var3, double var5, double var7, byte var9) {
      double var10 = 1.0;
      if (var9 == 3) {
         double var12 = Math.abs(var1 - var3);
         double var14 = var3 / 100.0;
         double var16 = var12 / var14;
         return 1.0 / (1.0 + var16 / 100.0);
      }

      if (var1 <= 0.0) {
         if (var5 == 0.0) {
            var10 = 1.0;
         }

         if (var1 != 0.0) {
            if (var5 < -8000.0) {
               var10 = 1.0 - 1200.0 / (Math.abs(var1) + 4000.0);
            } else {
               var10 = 1.0 - 0.3 / (Math.abs(var1) / (Math.abs(var5) / 10.0) + 1.0);
            }
         }
      } else {
         if (var9 == 1 && var5 == 0.0 && var7 == 1.0 && var1 <= 1.0 && var1 >= 0.0) {
            return var1;
         }

         if (var7 > 8000.0) {
            var10 = 2800.0 / (var1 + 4000.0);
         } else {
            var10 = 0.7 / (1.0 + var1 / (var7 / 10.0));
         }
      }

      if (var10 > 1.0) {
         var10 = 1.0;
      }

      if (var10 < 0.0) {
         var10 = 0.0;
      }

      return var9 == 1 ? 1.0 - var10 : var10;
   }
}
