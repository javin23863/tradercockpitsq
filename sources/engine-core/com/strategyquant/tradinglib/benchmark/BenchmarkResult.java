package com.strategyquant.tradinglib.benchmark;

public class BenchmarkResult {
   public static double TOTAL_TICKS = 1.3241836E7;
   public Throwable error;
   public String testName;
   public long testTime;
   public long dataLoadTime;
   public double strategyAverageTime;
   public double avgStrategiesPerHour;
   public int totalTrades;
   public double timePerTick;

   public BenchmarkResult(String var1) {
      this.testName = var1;
   }

   public void setError(Throwable var1) {
      this.error = var1;
   }

   public boolean isError() {
      return this.error != null;
   }

   public void setResult(long var1, long var3, double var5, double var7, int var9) {
      this.testTime = var1;
      this.dataLoadTime = var3;
      this.strategyAverageTime = var5;
      this.avgStrategiesPerHour = var7;
      this.totalTrades = var9;
      this.timePerTick = var5 / TOTAL_TICKS;
   }

   @Override
   public String toString() {
      return this.isError()
         ? String.format("%s\t|\tTEST ERROR: %s", this.testName, this.error.getMessage())
         : String.format(
            "%s\t|\t%d ms.\t|\t%.2f\t|\t%.2f\t|\t%d\t|\t%.10f",
            this.testName,
            this.testTime,
            this.strategyAverageTime,
            this.avgStrategiesPerHour,
            this.totalTrades,
            this.timePerTick
         );
   }
}
