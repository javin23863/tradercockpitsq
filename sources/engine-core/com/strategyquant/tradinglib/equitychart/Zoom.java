package com.strategyquant.tradinglib.equitychart;

public class Zoom {
   public boolean active = false;
   public int width = 1024;
   public long minX;
   public long maxX;

   public void reset() {
      this.active = false;
      this.width = 1024;
   }

   public Long getMin(long[] var1) {
      return this.active ? this.minX : var1.length > 0 ? var1[0] : 0L;
   }

   public Long getMax(long[] var1) {
      return this.active ? this.maxX : var1.length > 0 ? var1[var1.length - 1] : 0L;
   }
}
