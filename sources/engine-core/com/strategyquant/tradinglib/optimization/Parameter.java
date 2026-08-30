package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQTime;

public class Parameter {
   String name;
   byte type;
   double start;
   double stop;
   double step;
   double original;
   private int combinationsCount = -1;
   boolean use;

   Parameter(String var1, byte var2, boolean var3, double var4, double var6, double var8, double var10) {
      this.name = var1;
      this.type = var2;
      this.use = var3;
      this.start = var4;
      this.stop = var6;
      this.step = var8;
      this.original = var10;
      this.combinationsCount = this._getCombinationsCount();
   }

   public static byte getType(String var0) {
      switch (var0) {
         case "boolean":
            return 2;
         case "int":
            return 1;
         case "double":
            return 4;
         case "time":
            return 5;
         default:
            return 0;
      }
   }

   public byte getType() {
      return this.type;
   }

   public String getName() {
      return this.name;
   }

   public int getCombinationsCount() {
      return this.combinationsCount;
   }

   private int _getCombinationsCount() {
      if (!this.use) {
         return 1;
      } else if (this.type == 2) {
         return 2;
      } else if (this.type == 5) {
         int var1 = SQTime.HHMMToMinutes((int)this.start);
         int var2 = SQTime.HHMMToMinutes((int)this.stop);
         int var3 = SQTime.HHMMToMinutes((int)this.step);
         return var3 != 0 && var2 != var1 ? Math.abs((var2 - var1) / var3 + 1) : 1;
      } else {
         return this.step != 0.0 && this.stop != this.start ? (int)Math.abs(Math.round((this.stop - this.start) / this.step) + 1.0) : 1;
      }
   }

   public double getOriginalValue() {
      return this.original;
   }

   public double getStart() {
      return this.start;
   }

   public double getStop() {
      return this.stop;
   }

   public double getStep() {
      return this.step;
   }

   public boolean isUsed() {
      return this.use;
   }

   public double getValueByIndex(int var1) throws OptimizationException {
      if (var1 < 0) {
         return this.original;
      } else if (var1 > this.combinationsCount - 1) {
         throw new OptimizationException(String.format("Index out of range. Parameter: '%s', index: %d", this.name, var1));
      } else if (this.type == 2) {
         return var1 <= 0 ? (this.start > 0.0 ? 1 : 0) : (this.start > 0.0 ? 0 : 1);
      } else if (this.type == 5) {
         int var8 = SQTime.HHMMToMinutes((int)this.start);
         int var3 = SQTime.HHMMToMinutes((int)this.step);
         int var9 = var1 < 0 ? (int)this.original : var8 + var1 * var3;
         var9 = var9 < 0 ? 0 : (var9 > 1439 ? 1439 : var9);
         return SQTime.minutesToHHMM(var9);
      } else {
         double var2 = this.start;
         double var4 = this.step;
         double var6 = var1 < 0 ? this.original : var2 + var1 * var4;
         return this.type == 1 ? (int)var6 : var6;
      }
   }

   public Parameter clone() {
      return new Parameter(this.name, this.type, this.use, this.start, this.stop, this.step, this.original);
   }
}
