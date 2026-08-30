package com.strategyquant.tradinglib.setup;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingSetupRunnable extends Thread {
   public static final Logger Log = LoggerFactory.getLogger("TradingSetupRunnable");
   private static final int RUNSTATUS_BEFORESTART = 0;
   private static final int RUNSTATUS_RUNNING = 1;
   private static final int RUNSTATUS_STOPPING = 2;
   private static final int RUNSTATUS_STOPPED = 3;
   private static final int RUNSTATUS_FINISHED = 4;
   private static final int RUNSTATUS_EXCEPTION = 5;
   private static final long MAX_STOPPING_TIME = 2000L;
   protected TradingSetup tradingSetup;
   protected IDataBuffer dataBuffer;
   protected final BarUpdateInfo updateInfo = new BarUpdateInfo();
   protected long lastIndex;
   private Exception tradingException = null;
   StampedLock exceptionStampedLock = new StampedLock();
   private int runStatus = 0;
   StampedLock runStatusStampedLock = new StampedLock();
   private MarketData marketData = null;
   private boolean usePreparedData = true;
   private int barUpdateEventType;

   public TradingSetupRunnable(TradingSetup var1, boolean var2) {
      this.tradingSetup = var1;
      this.usePreparedData = var2;
      this.barUpdateEventType = var1.getBarEventType();
      this.lastIndex = -1L;
   }

   public void runUnused() {
      if (this.getRunStatus() != 0) {
         this.setTradingException(new Exception("Cannot restart this runner after it was finished or interrupted !"));
      } else {
         try {
            this.setRunStatus(1);
            this.dataBuffer = this.tradingSetup.getTradingEngine().getConnectionManager().getDataBuffer();
            TickEvent var2 = new TickEvent();

            int var1;
            do {
               long var3 = this.dataBuffer.getOne(this.lastIndex, var2);
               if (var3 != -1L) {
                  this.runOneCycle(var2, true);
                  this.lastIndex = var3;
               }

               var1 = this.getRunStatusUnlocked();
            } while (var1 != 2);

            this.setRunStatus(3);
            if (var1 != 3) {
               this.setRunStatus(4);
            }
         } catch (Exception var5) {
            this.setRunStatus(5);
            this.setTradingException(var5);
         }
      }
   }

   public void stopRunner() throws Exception {
      if (this.getRunStatus() == 1) {
         this.setRunStatus(2);
         long var1 = System.currentTimeMillis();

         while (true) {
            int var5 = this.getRunStatus();
            if (var5 == 3 || var5 == 4 || var5 == 5) {
               return;
            }

            long var3 = System.currentTimeMillis();
            if (var3 - var1 > 2000L) {
               this.interrupt();
               var1 = System.currentTimeMillis();

               while (!this.isInterrupted()) {
                  var3 = System.currentTimeMillis();
                  if (var3 - var1 > 2000L) {
                     throw new Exception("Cannot stop runner !");
                  }

                  try {
                     sleep(10L);
                  } catch (InterruptedException var7) {
                  }
               }

               this.setRunStatus(3);
               return;
            }

            try {
               sleep(10L);
            } catch (InterruptedException var8) {
            }
         }
      }
   }

   private void setRunStatus(int var1) {
      this.runStatus = var1;
   }

   public int getRunStatus() {
      return this.runStatus;
   }

   public int getRunStatusUnlocked() {
      return this.runStatus;
   }

   private void setTradingException(Exception var1) {
      this.tradingException = var1;
   }

   public Exception getTradingException() {
      return this.tradingException;
   }

   protected void runOneCycle(TickEvent var1, boolean var2) throws Exception {
      if (this.barUpdateEventType == 3) {
         if (!this.usePreparedData) {
            throw new Exception("Not implemented!");
         }

         this.processReceivedTickOnClosePrepared(var1, var2);
      } else if (this.usePreparedData) {
         this.processReceivedTickPrepared(var1, var2);
      } else {
         this.processReceivedTickRealtime(var1);
      }
   }

   private void processReceivedTickOnClosePrepared(TickEvent var1, boolean var2) throws Exception {
      if (this.marketData == null) {
         this.marketData = this.tradingSetup.getMarketData();
      }

      boolean var3 = false;
      this.updateInfo.updatedChart = -1;
      this.updateInfo.updatedMainChart = false;
      this.updateInfo.multipleChartsUpdated = false;
      this.updateInfo.eventType = 1;
      if (this.marketData.findTickInData(var1, this.updateInfo, 3, true, false)) {
         if (var2 && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 3)) {
            var3 = true;
         }

         if (this.marketData.isNextDay(var1)) {
            this.tradingSetup.callExitEOD(var1);
         }
      }

      this.tradingSetup.callOptionsOnTick(var1);
      this.marketData.findTickInData(var1, this.updateInfo, 2, false, false);
      if (!var3 && this.updateInfo.eventType != 1) {
         if (var2) {
            this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 4);
         }

         var3 = true;
      }
   }

   private void processReceivedTickPrepared(TickEvent var1, boolean var2) throws Exception {
      if (this.marketData == null) {
         this.marketData = this.tradingSetup.getMarketData();
      }

      boolean var3 = false;
      this.updateInfo.updatedChart = -1;
      this.updateInfo.updatedMainChart = false;
      this.updateInfo.multipleChartsUpdated = false;
      this.updateInfo.eventType = 1;
      this.tradingSetup.callOptionsOnTick(var1);
      if (this.marketData.findTickInData(var1, this.updateInfo, 2, false) && var2 && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 2)) {
         var3 = true;
      }

      if (!var3 && this.updateInfo.eventType != 1) {
         if (var2) {
            this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 4);
         }

         var3 = true;
      }
   }

   private void processReceivedTickWithCloseEvent(TickEvent var1) throws Exception {
      MarketData var2 = this.tradingSetup.getMarketData();
      boolean var3 = false;
      this.updateInfo.updatedChart = -1;
      this.updateInfo.multipleChartsUpdated = false;
      this.updateInfo.eventType = 1;
      this.tradingSetup.callOptionsOnTick(var1);
      if (var2.findTickInData(var1, this.updateInfo, 3, true) && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 3)) {
         var3 = true;
      }

      if (var2.findTickInData(var1, this.updateInfo, 2, true) && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 2)) {
         var3 = true;
      }

      if (!var3 && this.updateInfo.eventType != 1) {
         this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 4);
         var3 = true;
      }
   }

   private void processReceivedTickRealtime(TickEvent var1) throws Exception {
      MarketData var2 = this.tradingSetup.getMarketData();
      boolean var3 = false;
      this.updateInfo.updatedChart = -1;
      this.updateInfo.updatedMainChart = false;
      this.updateInfo.multipleChartsUpdated = false;
      this.updateInfo.eventType = 1;
      this.tradingSetup.callOptionsOnTick(var1);
      if (var2.processTick(var1, this.updateInfo, 3) && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 3)) {
         var3 = true;
      }

      if (var2.processTick(var1, this.updateInfo, 2) && this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 2)) {
         var3 = true;
      }

      if (!var3 && this.updateInfo.eventType != 1) {
         this.tradingSetup.callStrategyOnBarUpdate(this.updateInfo, 4);
         var3 = true;
      }
   }

   public void setBarEventType(int var1) {
      this.barUpdateEventType = var1;
   }
}
