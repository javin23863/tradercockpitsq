package com.strategyquant.indicatorTester;

import com.strategyquant.lib.SQUtils;
import org.json.JSONObject;

public class CalibrationRangesInfo {
   public static int maxSteps = -1;
   public String symbol;
   public String timeframe;
   public double maxValue;
   public double minValue;

   public CalibrationRangesInfo() {
   }

   public CalibrationRangesInfo(String var1, String var2) {
      this.symbol = var1;
      this.timeframe = var2;
   }

   public CalibrationRangesInfo(String var1, String var2, double var3, double var5) {
      this.symbol = var1;
      this.timeframe = var2;
      this.minValue = var3;
      this.maxValue = var5;
   }

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("symbol", this.symbol);
      var1.put("timeframe", this.timeframe);
      var1.put("minValue", this.minValue);
      var1.put("maxValue", this.maxValue);
      if (maxSteps > 0) {
         var1.put("step", this.getStep(this.minValue, this.maxValue));
      }

      return var1;
   }

   public void fixValues() {
      double var1 = Math.abs(this.maxValue - this.minValue);
      if (CalibrationConfig.disableRounding) {
         this.minValue = this.floor(this.minValue, var1 / 100.0);
         this.maxValue = this.ceil(this.maxValue, var1 / 100.0);
      } else {
         this.minValue = this.floor(this.minValue, var1);
         this.maxValue = this.ceil(this.maxValue, var1);
      }
   }

   private double getStep(double var1, double var3) {
      double var5 = (var3 - var1) / maxSteps;
      int var7 = SQUtils.getDecimalPlaces(var1);
      int var8 = SQUtils.getDecimalPlaces(var3);
      int var9 = SQUtils.getDecimalPlaces(var5);
      if (var9 > var7 && var9 > var8) {
         int var10 = 0;
         if (var5 > 100.0) {
            var10 = (byte)0;
         } else if (var5 > 10.0) {
            var10 = (byte)1;
         } else if (var5 > 1.0) {
            var10 = (byte)2;
         } else if (var5 > 0.1) {
            var10 = (byte)3;
         } else if (var5 > 0.01) {
            var10 = (byte)4;
         } else {
            var10 = (byte)5;
         }

         var10 = maxSteps > 100 ? var10 + 1 : var10;
         var10 = Math.min(var10, 5);
         return Math.max(1.0 / Math.pow(10.0, var10), SQUtils.round(var5, var10));
      } else {
         return SQUtils.round(var5, var9);
      }
   }

   private double ceil(double var1, double var3) {
      double var5 = Math.abs(var1);
      double var7 = Math.abs(var3);
      if (var5 / 100000.0 > 1.0 && var7 / 100000.0 > 1.0) {
         return (int)(Math.ceil(var1 / 10000.0) * 10000.0);
      } else if (var5 / 10000.0 > 1.0 && var7 / 10000.0 > 1.0) {
         return (int)(Math.ceil(var1 / 1000.0) * 1000.0);
      } else if (var5 / 1000.0 > 1.0 && var7 / 1000.0 > 1.0) {
         return (int)(Math.ceil(var1 / 100.0) * 100.0);
      } else if (var5 / 100.0 > 1.0 && var7 / 100.0 > 1.0) {
         return (int)(Math.ceil(var1 / 10.0) * 10.0);
      } else if (var5 / 10.0 > 1.0 && var7 / 10.0 > 1.0) {
         return (int)(Math.ceil(var1 / 1.0) * 1.0);
      } else if (var5 / 1.0 > 1.0 && var7 / 1.0 > 1.0) {
         return SQUtils.round(Math.ceil(var1 / 0.1) * 0.1, 1);
      } else if (var5 / 0.1 > 1.0 && var7 / 0.1 > 1.0) {
         return SQUtils.round(Math.ceil(var1 / 0.01) * 0.01, 2);
      } else if (var5 / 0.01 > 1.0 && var7 / 0.01 > 1.0) {
         return SQUtils.round(Math.ceil(var1 / 0.001) * 0.001, 3);
      } else if (var5 / 0.001 > 1.0 && var7 / 0.001 > 1.0) {
         return SQUtils.round(Math.ceil(var1 / 1.0E-4) * 1.0E-4, 4);
      } else {
         return var5 / 1.0E-4 > 1.0 && var7 / 1.0E-4 > 1.0 ? SQUtils.round(Math.ceil(var1 / 1.0E-5) * 1.0E-5, 5) : SQUtils.round(var1, 5);
      }
   }

   private double floor(double var1, double var3) {
      double var5 = Math.abs(var1);
      double var7 = Math.abs(var3);
      if (var5 / 100000.0 > 1.0 && var7 / 100000.0 > 1.0) {
         return (int)(Math.floor(var1 / 10000.0) * 10000.0);
      } else if (var5 / 10000.0 > 1.0 && var7 / 10000.0 > 1.0) {
         return (int)(Math.floor(var1 / 1000.0) * 1000.0);
      } else if (var5 / 1000.0 > 1.0 && var7 / 1000.0 > 1.0) {
         return (int)(Math.floor(var1 / 100.0) * 100.0);
      } else if (var5 / 100.0 > 1.0 && var7 / 100.0 > 1.0) {
         return (int)(Math.floor(var1 / 10.0) * 10.0);
      } else if (var5 / 10.0 > 1.0 && var7 / 10.0 > 1.0) {
         return (int)(Math.floor(var1 / 1.0) * 1.0);
      } else if (var5 / 1.0 > 1.0 && var7 / 1.0 > 1.0) {
         return SQUtils.round(Math.floor(var1 / 0.1) * 0.1, 1);
      } else if (var5 / 0.1 > 1.0 && var7 / 0.1 > 1.0) {
         return SQUtils.round(Math.floor(var1 / 0.01) * 0.01, 2);
      } else if (var5 / 0.01 > 1.0 && var7 / 0.01 > 1.0) {
         return SQUtils.round(Math.floor(var1 / 0.001) * 0.001, 3);
      } else if (var5 / 0.001 > 1.0 && var7 / 0.001 > 1.0) {
         return SQUtils.round(Math.floor(var1 / 1.0E-4) * 1.0E-4, 4);
      } else {
         return var5 / 1.0E-4 > 1.0 && var7 / 1.0E-4 > 1.0 ? SQUtils.round(Math.floor(var1 / 1.0E-5) * 1.0E-5, 5) : SQUtils.round(var1, 5);
      }
   }
}
