package com.strategyquant.tradinglib.blocks.annotations;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.blocks.random.BlockParameter;

public class Value {
   public static final String Null = "Null";
   public static final int MinShift = -1000001;
   public static final int MaxShift = -1000002;
   public static final int MinPeriod = -1000003;
   public static final int MaxPeriod = -1000004;
   public static final int MinSLInPips = -1000005;
   public static final int MaxSLInPips = -1000006;
   public static final int MinSLATRMultiple = -1000007;
   public static final int MaxSLATRMultiple = -1000008;
   public static final int MinSLATRPeriod = -1000009;
   public static final int MaxSLATRPeriod = -1000010;
   public static final int MinPTInPips = -1000011;
   public static final int MaxPTInPips = -1000012;
   public static final int MinPTATRMultiple = -1000013;
   public static final int MaxPTATRMultiple = -1000014;
   public static final int MinPTATRPeriod = -1000015;
   public static final int MaxPTATRPeriod = -1000016;
   public static final int MinPTInPercent = -1000017;
   public static final int MaxPTInPercent = -1000018;
   public static final int MinSLInPercent = -1000019;
   public static final int MaxSLInPercent = -1000020;

   public static boolean isPredefined(double var0) {
      return var0 == -1000001.0
         || var0 == -1000002.0
         || var0 == -1000003.0
         || var0 == -1000004.0
         || var0 == -1000005.0
         || var0 == -1000006.0
         || var0 == -1000019.0
         || var0 == -1000020.0
         || var0 == -1000007.0
         || var0 == -1000008.0
         || var0 == -1000009.0
         || var0 == -1000010.0
         || var0 == -1000011.0
         || var0 == -1000012.0
         || var0 == -1000017.0
         || var0 == -1000018.0
         || var0 == -1000013.0
         || var0 == -1000014.0
         || var0 == -1000015.0
         || var0 == -1000016.0;
   }

   public static int generateMM(IRandomGenerator var0, BlockParameter var1) {
      int var2 = (int)var1.minValue % 100;
      int var3 = (int)var1.maxValue % 100;
      int var4 = (int)var1.step % 100;
      if (var4 == 0) {
         return 0;
      }

      if (var2 < 0) {
         var2 = 0;
      }

      if (var3 > 59) {
         var3 = 59;
      }

      int var5;
      if (var3 >= var2) {
         var5 = (var3 - var2) / var4 + 1;
      } else {
         var5 = (var3 + var2) / var4 + 1;
      }

      int var6 = var0.nextInt(var5);
      int var7 = var2 + var6 * var4;
      if (var7 > 60) {
         var7 -= 60;
      }

      return var7 == 60 ? 0 : var7;
   }

   public static int generateHH(IRandomGenerator var0, BlockParameter var1) {
      int var2 = (int)var1.minValue / 100;
      int var3 = (int)var1.maxValue / 100;
      int var4 = (int)var1.step / 100;
      if (var4 == 0) {
         var4 = 1;
      }

      if (var2 < 0) {
         var2 = 0;
      }

      if (var3 > 23) {
         var3 = 23;
      }

      int var5 = (var3 - var2) / var4 + 1;
      int var6 = var0.nextInt(var5);
      return var2 + var6 * var4;
   }
}
