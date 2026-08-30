package com.strategyquant.tradinglib.project;

import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.lib.IStopPauseStatus;
import com.strategyquant.lib.L;
import com.strategyquant.lib.time.SQTimeOld;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopPauseEngine extends RunningStatusBase implements IStopPauseStatus {
   public static final Logger Log = LoggerFactory.getLogger(StopPauseEngine.class);
   private long startTime;
   private long elapsedTime;
   private ConcurrentLinkedQueue<IProgressStatusListener> listeners = new ConcurrentLinkedQueue<>();

   public void start() throws Exception {
      int var1 = this.getRunningStatus();
      if (!RunningStatus.canBeStarted(var1)) {
         Log.info("Cannot start project in incorrect running status: {}", var1);
      } else {
         this.startTime = System.currentTimeMillis();
         this.elapsedTime = 0L;
         this.setStatus(1);
      }
   }

   public void pause() {
      int var1 = this.getRunningStatus();
      if (var1 == 1) {
         this.printToLog(L.t("Project paused", new Object[0]));
         this.setStatus(2);
      }
   }

   public void resume() {
      int var1 = this.getRunningStatus();
      if (var1 == 2) {
         this.printToLog(L.t("Project resumed", new Object[0]));
         this.setStatus(1);
      }
   }

   public void stop() {
      this.stop(true);
   }

   public void stop(boolean var1) {
      int var2 = this.getRunningStatus();
      if (var2 == 1 || var2 == 2) {
         if (var1) {
            this.printToLog(L.t("Project stopped", new Object[0]));
         }

         this.setStatus(4);
      }
   }

   public void finish() {
      int var1 = this.getRunningStatus();
      if (var1 == 1 || var1 == 2) {
         this.setStatus(3);
      }
   }

   protected void setStatus(int var1) {
      this.setRunningStatus(var1);

      for (IProgressStatusListener var3 : this.listeners) {
         var3.onStatusChanged(var1);
      }
   }

   public void registerStatusListener(IProgressStatusListener var1) {
      if (!this.listeners.contains(var1)) {
         this.listeners.add(var1);
      }
   }

   public void unregisterStatusListener(IProgressStatusListener var1) {
      this.listeners.remove(var1);
   }

   public void done() {
      this.elapsedTime = System.currentTimeMillis() - this.startTime;
      String var1 = SQTimeOld.formatDateTime((int)(this.elapsedTime / 1000L));
      this.printToLog(L.t("Finished in %s", new Object[]{var1}));
   }

   public boolean isStopped() {
      int var1 = this.getRunningStatus();
      return var1 == 4 || var1 == 3 || var1 == 50;
   }

   public boolean isStoppedNotFinished() {
      int var1 = this.getRunningStatus();
      return var1 == 4 || var1 == 50;
   }

   public boolean isPaused() {
      int var1 = this.getRunningStatus();
      return var1 == 2;
   }

   public boolean isFinished() {
      int var1 = this.getRunningStatus();
      return var1 == 3;
   }

   public boolean checkRunning() {
      int var1 = this.getRunningStatus();
      return var1 == 1;
   }

   public void checkPaused() {
      if (this.isPaused()) {
         while (this.isPaused()) {
            try {
               Thread.sleep(100L);
            } catch (InterruptedException var2) {
               var2.printStackTrace();
            }
         }
      }
   }

   public void printToLog(String var1) {
   }

   public void printToLogDebug(String var1) {
   }

   public void reset() {
      this.setRunningStatus(0);
   }

   public void error() {
      this.setStatus(50);
   }
}
