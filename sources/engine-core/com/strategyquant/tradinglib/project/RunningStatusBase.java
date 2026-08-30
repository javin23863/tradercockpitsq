package com.strategyquant.tradinglib.project;

import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningStatusBase {
   public static final Logger Log = LoggerFactory.getLogger(RunningStatusBase.class);
   private int runningStatus = 0;
   private StampedLock stampedLock = new StampedLock();
   private ProjectStatusListener listener = null;

   public int getRunningStatus() {
      long var1 = this.stampedLock.tryOptimisticRead();
      int var3 = this.runningStatus;
      if (!this.stampedLock.validate(var1)) {
         var1 = this.stampedLock.readLock();

         try {
            var3 = this.runningStatus;
         } finally {
            this.stampedLock.unlockRead(var1);
         }
      }

      return var3;
   }

   public void setRunningStatus(int var1) {
      long var2 = this.stampedLock.writeLock();

      try {
         this.runningStatus = var1;
         if (this.listener != null) {
            this.listener.projectStatusChanged(var1);
         }
      } finally {
         this.stampedLock.unlockWrite(var2);
      }
   }

   public void setProjectStatusListener(ProjectStatusListener var1) {
      this.listener = var1;
   }
}
