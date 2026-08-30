package com.strategyquant.datalib.data.impl;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.IDataBuffer;
import java.util.concurrent.locks.StampedLock;

public class ConcurrentDataBuffer implements IDataBuffer {
   private TickEvent[] ring;
   private int capacity;
   private long index = -1L;
   StampedLock stampedLock = new StampedLock();

   public ConcurrentDataBuffer(int var1) {
      this.capacity = var1;
      this.ring = new TickEvent[var1];

      for (int var2 = 0; var2 < var1; var2++) {
         this.ring[var2] = new TickEvent();
      }
   }

   @Override
   public void put(TickEvent var1) {
      long var2 = this.stampedLock.writeLock();

      try {
         this.index++;
         int var4 = (int)(this.index % this.capacity);
         this.ring[var4].copyValues(var1);
      } finally {
         this.stampedLock.unlockWrite(var2);
      }
   }

   @Override
   public void printRing() {
      String var1 = "Index: " + this.index + " - ";

      for (int var2 = 0; var2 < this.capacity; var2++) {
         var1 = var1 + this.ring[var2].getAsk() + ",";
      }

      System.out.println(var1);
   }

   @Override
   public long get(long var1, TickEvent[] var3) {
      long var4 = this.stampedLock.tryOptimisticRead();
      long var6 = this._get(var1, var3);
      if (!this.stampedLock.validate(var4)) {
         var4 = this.stampedLock.readLock();

         try {
            var6 = this._get(var1, var3);
         } finally {
            this.stampedLock.unlockRead(var4);
         }
      }

      return var6;
   }

   private long _get(long var1, TickEvent[] var3) {
      if (this.index == -1L) {
         return -1L;
      }

      if (var1 >= this.index) {
         return -1L;
      }

      long var4 = this.index - this.capacity;
      if (var1 >= 0L && var1 > var4) {
         var4 = var1;
      }

      if (++var4 < 0L) {
         var4 = 0L;
      }

      long var6 = var3.length;
      long var8 = -1L;
      int var10 = 0;

      for (var8 = var4; var8 <= this.index; var8++) {
         if (var10 == var6) {
            return var8 - 1L;
         }

         int var11 = (int)(var8 % this.capacity);
         if (var10 < 0) {
            System.out.println("Error J < 0 = " + var10);
         }

         if (var11 < 0) {
            System.out.println("Error realPos < 0 = " + var11);
            System.out.println("i=" + var8 + ", capacity=" + this.capacity + ", lastIndex=" + var1 + ", index=" + this.index + ", startIndex=" + var4);
         }

         var3[var10].copyValues(this.ring[var11]);
         var3[var10].setIsSet(true);
         var10++;
      }

      if (var10 < var6) {
         var3[var10].setIsSet(false);
         return var8 - 1L;
      } else {
         return var8 - 1L;
      }
   }

   @Override
   public long getOne(long var1, TickEvent var3) {
      long var4 = this.stampedLock.tryOptimisticRead();
      long var6 = this._getOne(var1, var3);
      if (!this.stampedLock.validate(var4)) {
         var4 = this.stampedLock.readLock();

         try {
            var6 = this._getOne(var1, var3);
         } finally {
            this.stampedLock.unlockRead(var4);
         }
      }

      return var6;
   }

   private long _getOne(long var1, TickEvent var3) {
      if (this.index == -1L) {
         return -1L;
      }

      if (var1 >= this.index) {
         return -1L;
      }

      long var4 = this.index - this.capacity;
      if (var1 >= 0L && var1 > var4) {
         var4 = var1;
      }

      if (++var4 < 0L) {
         var4 = 0L;
      }

      boolean var6 = false;
      int var7 = (int)(var4 % this.capacity);
      var3.copyValues(this.ring[var7]);
      var3.setIsSet(true);
      return var4;
   }
}
