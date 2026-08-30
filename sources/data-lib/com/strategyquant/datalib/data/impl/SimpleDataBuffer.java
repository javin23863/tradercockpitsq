package com.strategyquant.datalib.data.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;

public class SimpleDataBuffer implements IDataBuffer {
   private TickEvent oneEvent = new TickEvent();
   private long index = -1L;

   @Override
   public void put(TickEvent var1) {
      this.index++;
      this.oneEvent.copyValues(var1);
   }

   @Override
   public void printRing() {
      String var1 = "Index: " + this.index + " - ";
      var1 = var1 + this.oneEvent.getAsk() + ",";
      System.out.println(var1);
   }

   @Override
   public long get(long var1, TickEvent[] var3) {
      if (this.index == -1L) {
         return -1L;
      }

      if (var1 >= this.index) {
         return -1L;
      }

      var3[0].copyValues(this.oneEvent);
      var3[0].setIsSet(true);
      return var1 + 1L;
   }

   @Override
   public long getOne(long var1, TickEvent var3) {
      if (this.index == -1L) {
         return -1L;
      }

      if (var1 >= this.index) {
         return -1L;
      }

      var3.copyValues(this.oneEvent);
      var3.setIsSet(true);
      return var1 + 1L;
   }
}
