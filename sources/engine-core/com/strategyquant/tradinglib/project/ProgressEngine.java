package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.databank.IProgressListener;
import java.util.ArrayList;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressEngine extends StopPauseEngine {
   public static final Logger Log = LoggerFactory.getLogger(ProgressEngine.class);
   private String projectName;
   private String taskName;
   protected long progress = 0L;
   protected long maxProgress;
   private ArrayList<IProgressListener> listeners = new ArrayList<>();
   private String logPrefix = null;
   private int previousMessageHash = 0;
   StampedLock stampedLock = new StampedLock();

   public ProgressEngine(String var1) {
      this.projectName = var1;
   }

   @Override
   public void printToLogDebug(String var1) {
      if (var1 != null) {
         long var2 = this.stampedLock.writeLock();

         try {
            if (this.logPrefix != null) {
               var1 = this.logPrefix + " : " + var1;
            }

            if (var1.hashCode() != this.previousMessageHash) {
               Log.debug(var1);
               SQLogger.log(this.projectName, var1);
               this.previousMessageHash = var1.hashCode();
            }
         } finally {
            this.stampedLock.unlockWrite(var2);
         }
      }
   }

   @Override
   public void printToLog(String var1) {
      if (var1 != null) {
         long var2 = this.stampedLock.writeLock();

         try {
            if (this.logPrefix != null) {
               var1 = this.logPrefix + " : " + var1;
            }

            if (var1.hashCode() != this.previousMessageHash) {
               Log.info(var1);
               SQLogger.log(this.projectName, var1);
               this.previousMessageHash = var1.hashCode();
            }
         } finally {
            this.stampedLock.unlockWrite(var2);
         }
      }
   }

   public void reset(String var1, long var2) {
      this.taskName = var1;
      this.maxProgress = var2;
      this.setProgress(0L);
   }

   public void setProgress(long var1) {
      this.progress = var1;

      for (int var3 = 0; var3 < this.listeners.size(); var3++) {
         this.listeners.get(var3).onProgress(var1);
      }
   }

   public double getMaxProgress() {
      return this.maxProgress;
   }

   public void setLogPrefix(String var1) {
      this.logPrefix = var1;
   }

   @Override
   public void finish() {
      this.finish(0L);
   }

   public void finish(long var1) {
      int var3 = this.getRunningStatus();
      if (var3 == 1 || var3 == 2 || var3 == 2) {
         this.setStatus(3);
         String var4;
         if (this.logPrefix == null) {
            var4 = L.t("Project finished", new Object[0]);
         } else {
            var4 = L.t("Task finished", new Object[0]);
         }

         if (var1 != 0L) {
            var4 = var4 + String.format(" in %.2f s.", var1 / 1000.0);
         }

         this.printToLog(var4);
      }
   }

   public void registerProgressListener(IProgressListener var1) {
      if (!this.listeners.contains(var1)) {
         this.listeners.add(var1);
      }
   }

   public String getLogPrefix() {
      return this.logPrefix;
   }

   public void setProjectName(String var1) {
      this.projectName = var1;
   }
}
