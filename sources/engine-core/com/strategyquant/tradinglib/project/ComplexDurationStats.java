package com.strategyquant.tradinglib.project;

public class ComplexDurationStats {
   public String name = "Unknown";
   public int count = 0;
   public double avgTime = 0.0;
   public double totalTime = 0.0;

   public void update(String var1, double var2) {
      this.name = var1;
      this.totalTime += var2;
      this.avgTime = (this.avgTime * this.count + var2) / (this.count + 1);
      this.count++;
   }
}
