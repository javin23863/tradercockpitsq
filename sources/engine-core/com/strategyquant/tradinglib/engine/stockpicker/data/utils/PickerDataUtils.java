package com.strategyquant.tradinglib.engine.stockpicker.data.utils;

import com.strategyquant.datalib.data.io.VersatileData;

public class PickerDataUtils {
   public static float computeWeightedValue(VersatileData var0) {
      return (float)(var0.high + var0.low + var0.close + var0.close) / 4.0F;
   }

   public static float computeTypicalValue(VersatileData var0) {
      return (float)(var0.high + var0.low + var0.close) / 3.0F;
   }

   public static float computeMedianValue(VersatileData var0) {
      return (float)(var0.high + var0.low) / 2.0F;
   }

   public static float computeWeightedValue(float var0, float var1, float var2, float var3) {
      return (var1 + var2 + var3 + var3) / 4.0F;
   }

   public static float computeTypicalValue(float var0, float var1, float var2, float var3) {
      return (var1 + var2 + var3) / 3.0F;
   }

   public static float computeMedianValue(float var0, float var1, float var2, float var3) {
      return (var1 + var2) / 2.0F;
   }
}
