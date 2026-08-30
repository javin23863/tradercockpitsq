package com.strategyquant.tradinglib.event;

import com.strategyquant.tradinglib.ILiveOrder;

public class TradingEventObj implements ITradingEvent {
   private int eventType;
   private ILiveOrder order;
   private boolean inUse;

   public void setEventType(int var1) {
      this.eventType = var1;
   }

   public void setOrder(ILiveOrder var1) {
      this.order = var1;
   }

   public synchronized void releaseEvent() {
      this.inUse = false;
   }

   @Override
   public synchronized boolean checkAndSetInUse() {
      if (this.inUse) {
         return false;
      }

      this.inUse = true;
      return true;
   }
}
