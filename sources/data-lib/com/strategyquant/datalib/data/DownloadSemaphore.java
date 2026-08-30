package com.strategyquant.datalib.data;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadSemaphore {
   private static final Logger Log = LoggerFactory.getLogger(DownloadSemaphore.class);
   private Semaphore semaphore = new Semaphore(1);
   private static final DownloadSemaphore instance = new DownloadSemaphore();

   public static DownloadSemaphore getInstance() {
      return instance;
   }

   public boolean startDownloadData(int var1) throws InterruptedException {
      Log.info("Acquiring semaphore");
      boolean var2 = this.semaphore.tryAcquire(var1, TimeUnit.SECONDS);
      Log.info("Semaphore acquired with result: {}", var2);
      return var2;
   }

   public void downloadFinished() {
      this.semaphore.release();
      Log.info("Semaphore released");
   }
}
