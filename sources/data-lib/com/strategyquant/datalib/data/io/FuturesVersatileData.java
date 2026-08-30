package com.strategyquant.datalib.data.io;

import com.strategyquant.lib.SQTime;

public class FuturesVersatileData extends VersatileData {
   public double openInterest;

   @Override
   public void reset() {
      super.reset();
      this.openInterest = 0.0;
   }

   public void set(int var1, long var2, int var4, int var5, double var6, double var8, double var10, double var12, double var14, double var16) {
      super.set(var1, var2, var4, var5, var6, var8, var10, var12, var14);
      this.openInterest = var16;
   }

   public void copyFrom(FuturesVersatileData var1) {
      super.copyFrom(var1);
      this.openInterest = var1.openInterest;
   }

   @Override
   public int getCheckSum() {
      return super.getCheckSum() + (int)this.openInterest;
   }

   @Override
   public String print() {
      return "type: "
         + this.type
         + ", time: "
         + this.time
         + ", ask: "
         + this.ask
         + ", bid: "
         + this.bid
         + ", open: "
         + this.open
         + ", high: "
         + this.high
         + ", low: "
         + this.low
         + ", close: "
         + this.close
         + ", volume: "
         + this.volume
         + ", openInterest:"
         + this.openInterest
         + ", symbolHash: "
         + this.symbolHash
         + ", connectionHash: "
         + this.connectionHash;
   }

   @Override
   public String toString() {
      return "FuturesVersatileData [time:" + SQTime.toDateMinuteString(this.time);
   }
}
