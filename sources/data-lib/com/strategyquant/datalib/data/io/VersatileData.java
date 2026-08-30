package com.strategyquant.datalib.data.io;

public class VersatileData {
   public static final int UNKNOWN_DATA = 0;
   public static final int TICK_DATA = 1;
   public static final int MINUTE_DATA = 2;
   public static final int DT_VOLUME = 1;
   public static final int DT_ASK = 2;
   public static final int DT_BID = 3;
   public static final int DT_OPEN = 4;
   public static final int DT_HIGH = 5;
   public static final int DT_LOW = 6;
   public static final int DT_CLOSE = 7;
   public static final int DT_INDICATOR_VALUE = 8;
   public static final int DT_TIME = 9;
   public static final int DT_UNUSED = 10;
   public static final int DT_CUSTOM_VALUE = 9999;
   public static final int ASK = 0;
   public static final int BID = 1;
   public static final long TimeNotDefined = Long.MIN_VALUE;
   public long time = Long.MIN_VALUE;
   public double ask;
   public double bid;
   public double open;
   public double high;
   public double low;
   public double close;
   public double volume;
   public double spread = 0.0;
   public int type;
   public int symbolHash;
   public int connectionHash;
   public long sessionStartTime;

   public void reset() {
      this.time = 0L;
      this.ask = 0.0;
      this.bid = 0.0;
      this.open = 0.0;
      this.high = 0.0;
      this.low = 0.0;
      this.close = 0.0;
      this.volume = 0.0;
      this.spread = 0.0;
   }

   public void set(int var1, long var2, int var4, int var5, double var6, double var8, double var10, double var12, double var14) {
      this.type = var1;
      this.time = var2;
      this.connectionHash = var4;
      this.symbolHash = var5;
      this.open = var6;
      this.high = var8;
      this.low = var10;
      this.close = var12;
      this.volume = var14;
   }

   public void set(int var1, long var2, int var4, int var5, double var6, double var8, double var10) {
      this.type = var1;
      this.time = var2;
      this.connectionHash = var4;
      this.symbolHash = var5;
      this.ask = var6;
      this.bid = var8;
      this.volume = var10;
   }

   public void copyFrom(VersatileData var1) {
      this.time = var1.time;
      this.ask = var1.ask;
      this.bid = var1.bid;
      this.open = var1.open;
      this.high = var1.high;
      this.low = var1.low;
      this.close = var1.close;
      this.volume = var1.volume;
      this.type = var1.type;
      this.symbolHash = var1.symbolHash;
      this.connectionHash = var1.connectionHash;
      this.sessionStartTime = var1.sessionStartTime;
   }

   public int getCheckSum() {
      return (int)(this.time + this.ask + this.bid + this.open + this.high + this.low + this.close + this.volume + this.symbolHash + this.connectionHash);
   }

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
         + ", symbolHash: "
         + this.symbolHash
         + ", connectionHash: "
         + this.connectionHash;
   }
}
