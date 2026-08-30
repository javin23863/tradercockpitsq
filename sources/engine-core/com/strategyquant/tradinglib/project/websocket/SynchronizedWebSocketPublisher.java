package com.strategyquant.tradinglib.project.websocket;

import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SynchronizedWebSocketPublisher implements WebSocketPublisher {
   private static final Logger Log = LoggerFactory.getLogger(SynchronizedWebSocketPublisher.class);
   private DataToSend dataToSend;
   private boolean dataPulled = false;
   private StampedLock lock = new StampedLock();

   @Override
   public final DataToSend getDataToSend() throws Exception {
      long var1 = this.lock.readLock();

      try {
         if (!this.dataPulled) {
            this.dataPulled = true;

            try {
               this.dataToSend = this.getData();
            } catch (Exception var7) {
               Log.error("Cannot get data from publisher '" + this.getClass().getSimpleName() + "'", var7);
               this.dataToSend = null;
            }
         }

         return this.dataToSend;
      } finally {
         this.lock.unlock(var1);
      }
   }

   @Override
   public final void confirmBroadcast() {
      long var1 = this.lock.writeLock();

      try {
         this.handleConfirmBroadcast();
      } finally {
         this.lock.unlock(var1);
      }
   }

   protected void handleConfirmBroadcast() {
      this.dataPulled = false;
   }

   protected abstract DataToSend getData();
}
