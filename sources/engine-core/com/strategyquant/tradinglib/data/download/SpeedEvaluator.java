package com.strategyquant.tradinglib.data.download;

import com.strategyquant.tradinglib.dukascopy.DownloadResultDto;

public class SpeedEvaluator {
   private static final double NANO_TO_SECS = 1.0E9;
   private static final double KBS_CONST = 1024.0;
   private volatile int avgDownloadSpeedCounter = 0;
   private volatile double avgDownloadSpeed = 0.0;
   private Object downloadSpeedLock = new Object();

   public void evalSpeed(DownloadResultDto var1) {
      long var2 = var1.getDownloadedSize();
      long var4 = var1.getDuration();
      if (var2 > 0L && var4 > 0L) {
         synchronized (this.downloadSpeedLock) {
            double var7 = var2 * 8.0 / 1024.0 / (var4 / 1.0E9);
            this.avgDownloadSpeed += var7;
            this.avgDownloadSpeedCounter++;
         }
      }
   }

   public void reset() {
      this.avgDownloadSpeedCounter = 0;
      this.avgDownloadSpeed = 0.0;
   }

   public String getSpeed() {
      double var1 = this.avgDownloadSpeed / this.avgDownloadSpeedCounter;
      String var3 = "Kbps";
      if (var1 > 1024.0) {
         var3 = "Mbps";
         var1 /= 1024.0;
      }

      double var4 = Math.round(var1 * 10.0) / 10.0;
      return var4 + var3;
   }
}
