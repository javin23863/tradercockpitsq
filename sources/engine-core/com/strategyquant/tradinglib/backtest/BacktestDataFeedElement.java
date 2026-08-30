package com.strategyquant.tradinglib.backtest;

public class BacktestDataFeedElement {
   static final byte TYPE_UNKNOWN = 0;
   static final byte TYPE_TICK = 1;
   static final byte TYPE_OHLC = 2;
   private byte type;
   private byte hashPairIndex;
   private long time;
   private double data1;
   private double data2;
   private double data3;
   private double[] dataX;
   private long sessionStartTime;

   public void setTickData(long var1, byte var3, double var4, double var6, double var8, long var10) {
      this.type = 1;
      this.time = var1;
      this.hashPairIndex = var3;
      this.data1 = var4;
      this.data2 = var6;
      this.data3 = var8;
      this.sessionStartTime = var10;
   }

   public void setOhlcData(long var1, byte var3, double var4, double var6, double var8, double var10, double var12, long var14) {
      this.type = 2;
      this.time = var1;
      this.hashPairIndex = var3;
      this.data1 = var4;
      this.data2 = var6;
      this.data3 = var12;
      this.dataX = new double[2];
      this.dataX[0] = var8;
      this.dataX[1] = var10;
      this.sessionStartTime = var14;
   }

   public void reset() {
      this.type = 0;
      this.dataX = null;
   }

   public void setFromElement(BacktestDataFeedElement var1) {
      this.type = var1.type;
      this.time = var1.time;
      this.hashPairIndex = var1.hashPairIndex;
      if (this.type == 1) {
         this.data1 = var1.data1;
         this.data2 = var1.data2;
         this.data3 = var1.data3;
      } else {
         this.data1 = var1.data1;
         this.data2 = var1.data2;
         this.data3 = var1.data3;
         if (this.dataX == null) {
            this.dataX = new double[2];
         }

         for (int var2 = 0; var2 < 2; var2++) {
            this.dataX[var2] = var1.dataX[var2];
         }
      }
   }

   public byte getType() {
      return this.type;
   }

   public long getTime() {
      return this.time;
   }

   public byte getHashPairIndex() {
      return this.hashPairIndex;
   }

   public double getOpen() {
      return this.data1;
   }

   public double getHigh() {
      return this.data2;
   }

   public double getLow() {
      return this.dataX[0];
   }

   public double getClose() {
      return this.dataX[1];
   }

   public double getVolume() {
      return this.data3;
   }

   public double getBid() {
      return this.data1;
   }

   public double getAsk() {
      return this.data2;
   }

   public long getSessionStartTime() {
      return this.sessionStartTime;
   }
}
