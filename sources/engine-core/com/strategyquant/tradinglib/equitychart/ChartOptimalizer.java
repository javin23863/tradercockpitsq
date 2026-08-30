package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.SQTime;

public class ChartOptimalizer {
   private static final long DAY = 86400000L;
   private static final int MAX = 2000;
   private long zoomMin;
   private long zoomMax;
   private Long dataMin;
   private Long dataMax;
   private long[] pixelGroupsX = new long[2000];
   private int[] pixelGroupsCount = new int[2000];
   private double[] pixelGroupsTotal = new double[2000];
   private boolean timeX;

   public ChartOptimalizer.OptimizerResult optimalize(boolean var1, long[] var2, double[] var3, Long var4, Long var5, int var6) {
      if (var2.length != var3.length) {
         throw new IllegalArgumentException("XVals and YVals must have same size.");
      }

      if (var2.length != 0 && var6 >= 0) {
         this.timeX = var1;
         this.zoomMin = var4 != null ? var4 : -1L;
         this.zoomMax = var5 != null ? var5 : -1L;
         if (var1) {
            this.zoomMin = SQTime.correctDayStart(this.zoomMin);
            this.zoomMax = SQTime.correctDayStart(this.zoomMax);
         }

         this.countMinMax(var2);
         return this.perform(var2, var3, var6);
      } else {
         return new ChartOptimalizer.OptimizerResult(new long[0], new double[0]);
      }
   }

   private ChartOptimalizer.OptimizerResult perform(long[] var1, double[] var2, int var3) {
      long var4 = this.dataMax - this.dataMin + 1L;
      if (var3 > 2000) {
         var3 = 2000;
      }

      double var6;
      if (this.timeX) {
         long var8 = var4 / 86400000L + 1L;
         if (var3 >= var8) {
            var6 = 8.64E7;
         } else {
            double var10 = (double)var8 / var3;
            double var12 = Math.ceil(var10);
            var6 = 86400000L * (long)var12;
            if (var6 == 0.0) {
               var6 = 8.64E7;
            }
         }
      } else {
         var6 = (double)var4 / var3;
      }

      int var18 = this.preparePixelGroups(var3, var6);
      int var9 = var1.length;
      int var19 = -1;
      int var11 = -1;

      for (int var20 = 0; var20 < var9; var20++) {
         long var13 = var1[var20];
         if (this.timeX) {
            var13 = SQTime.correctDayStart(var13);
         }

         if (this.fit(var13)) {
            var19 = var20;
            if (var11 == -1) {
               var11 = var20;
            }

            double var15 = ((double)var13 - this.dataMin.longValue()) / var6;
            int var17 = (int)Math.round(var15);
            if (var17 >= var18) {
               var17 = var18 - 1;
            }

            if (var17 < 0) {
               var17 = 0;
            }

            this.pixelGroupsCount[var17]++;
            this.pixelGroupsTotal[var17] = this.pixelGroupsTotal[var17] + var2[var20];
         }
      }

      int var21 = this.getPixelsCount();
      long[] var22 = new long[var21];
      double[] var14 = new double[var21];
      int var23 = 0;

      for (int var16 = 0; var16 < var18; var16++) {
         if (this.pixelGroupsCount[var16] != 0) {
            var22[var23] = this.pixelGroupsX[var16];
            var14[var23] = this.pixelGroupsTotal[var16] / this.pixelGroupsCount[var16];
            var23++;
         }
      }

      if (var19 != -1) {
         var14[var21 - 1] = var2[var19];
         var22[var21 - 1] = var1[var19];
      }

      if (var11 != -1) {
         var14[0] = var2[var11];
         var22[0] = var1[var11];
      }

      this.cleanup();
      return new ChartOptimalizer.OptimizerResult(var22, var14);
   }

   private void cleanup() {
      for (int var1 = 0; var1 < this.pixelGroupsCount.length; var1++) {
         this.pixelGroupsCount[var1] = 0;
         this.pixelGroupsTotal[var1] = 0.0;
         this.pixelGroupsX[var1] = 0L;
      }
   }

   private int getPixelsCount() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.pixelGroupsCount.length; var2++) {
         if (this.pixelGroupsCount[var2] > 0) {
            var1++;
         }
      }

      return var1;
   }

   private void countMinMax(long[] var1) {
      long var2 = Long.MAX_VALUE;
      long var4 = Long.MIN_VALUE;

      for (long var9 : var1) {
         if (var2 == Long.MAX_VALUE || var2 > var9) {
            var2 = var9;
         }

         if (var4 == Long.MIN_VALUE || var4 < var9) {
            var4 = var9;
         }
      }

      this.dataMin = var2;
      this.dataMax = var4;
      if (this.timeX) {
         this.dataMin = SQTime.correctDayStart(this.dataMin);
         this.dataMax = SQTime.correctDayStart(this.dataMax);
      }

      if (this.zoomMin > 0L && this.dataMin < this.zoomMin) {
         this.dataMin = this.zoomMin;
      }

      if (this.zoomMax > 0L && this.dataMax > this.zoomMax) {
         this.dataMax = this.zoomMax;
      }
   }

   private boolean fit(long var1) {
      return this.zoomMin > 0L && this.zoomMax > 0L ? var1 <= this.zoomMax && var1 >= this.zoomMin : true;
   }

   private int preparePixelGroups(int var1, double var2) {
      int var4 = 0;

      for (int var5 = 0; var5 < var1; var5++) {
         long var6;
         if (this.timeX) {
            var6 = this.dataMin + (long)var2 * var5;
         } else {
            double var8 = this.dataMin.longValue() + var2 * var5;
            var6 = Math.round(var8);
         }

         this.pixelGroupsX[var5] = var6;
         var4++;
      }

      return var4;
   }

   public static void main(String[] var0) {
      int var1 = 10000000;
      long[] var2 = new long[var1];
      double[] var3 = new double[var1];

      for (int var4 = 0; var4 < var1; var4++) {
         var2[var4] = var4;
         var3[var4] = Math.random() * 1000.0;
      }

      short var6 = 1000;
      System.out.println("Starting...");
      ChartOptimalizer.OptimizerResult var5 = new ChartOptimalizer().optimalize(false, var2, var3, 0L, (long)var6, var6);
      System.out.println("4. finished " + var5);
   }

   public static class OptimizerResult {
      public long[] xVals;
      public double[] yVals;

      public OptimizerResult() {
      }

      public OptimizerResult(long[] var1, double[] var2) {
         this.xVals = var1;
         this.yVals = var2;
      }

      public long[] getxVals() {
         return this.xVals;
      }

      public double[] getyVals() {
         return this.yVals;
      }

      public int size() {
         return this.xVals == null ? 0 : this.xVals.length;
      }

      @Override
      public String toString() {
         StringBuilder var1 = new StringBuilder();

         for (int var2 = 0; var2 < this.xVals.length; var2++) {
            var1.append(this.xVals[var2] + "," + this.yVals[var2] + "\n");
         }

         return "Result[xVals:" + this.xVals.length + ", yVals:" + this.yVals.length + "]\n" + var1.toString();
      }
   }

   private static class PixelGroup {
      private long xValue;
      private double total = 0.0;
      private int count = 0;

      public void prepare(long var1) {
         this.xValue = var1;
      }

      public void reset() {
         this.count = 0;
         this.total = 0.0;
      }

      public void add(double var1) {
         this.count++;
         this.total += var1;
      }

      public long getXValue() {
         return this.xValue;
      }

      public boolean hasValue() {
         return this.count != 0;
      }

      public double getYValues() {
         return this.total / this.count;
      }
   }
}
