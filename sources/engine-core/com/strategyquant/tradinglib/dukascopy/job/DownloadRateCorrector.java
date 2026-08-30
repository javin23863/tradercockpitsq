package com.strategyquant.tradinglib.dukascopy.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadRateCorrector {
   private static final Logger LOGGER = LoggerFactory.getLogger(DownloadRateCorrector.class);
   private volatile int successCount = 0;
   private volatile int failedCount = 0;
   private volatile long downloadDelay = 50L;
   private static final Long MINIMAL_DELAY = 1L;
   private int SUCCESS_COUNT_THREASHOLD = 100;
   private int FAILED_COUNT_THREASHOLD = 1;
   private volatile long lastDownloadTime = 0L;
   private Thread waitingThread;
   private volatile boolean stopped;

   public void downloadFailed() {
      this.failedCount++;
      if (this.failedCount >= this.FAILED_COUNT_THREASHOLD) {
         long var1 = (long)(this.downloadDelay == 0L ? MINIMAL_DELAY.longValue() : this.downloadDelay * 1.25);
         this.downloadDelay = var1;
         LOGGER.debug("Increasing waiting period to:" + var1);
         this.successCount = 0;
         this.failedCount = 0;
      }
   }

   public void downloadSuccess() {
      this.successCount++;
      if (this.successCount >= this.SUCCESS_COUNT_THREASHOLD) {
         this.successCount = 0;
         this.failedCount = 0;
         long var1 = (long)(this.downloadDelay * 0.75);
         this.downloadDelay = var1;
         LOGGER.debug("Decreasing waiting period to:" + var1);
      }
   }

   public synchronized void waitBeforeNextDownload() throws InterruptedException {
      if (this.stopped) {
         throw new InterruptedException();
      }

      long var1 = System.currentTimeMillis() - this.lastDownloadTime;
      long var3 = this.downloadDelay - var1;
      if (var3 > 0L) {
         LOGGER.debug("Waiting for:" + var3);
         this.waitingThread = Thread.currentThread();
         Thread.sleep(var3);
         this.waitingThread = null;
      }

      this.lastDownloadTime = System.currentTimeMillis();
   }

   public synchronized void stop() {
      this.stopped = true;
      if (this.waitingThread != null) {
         this.waitingThread.interrupt();
      }
   }
}
