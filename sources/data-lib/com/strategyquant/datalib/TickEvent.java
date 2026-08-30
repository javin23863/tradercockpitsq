package com.strategyquant.datalib;

public class TickEvent {
   private boolean isSet = false;
   private int symbolHash;
   private int connectionHash;
   private long time = -1L;
   private double ask;
   private double bid;
   private double volume;
   private long sessionStartTime;
   private long sessionEndTime;
   private boolean barOpen = false;
   private boolean barClose = false;

   public boolean isSet() {
      return this.isSet;
   }

   public void setIsSet(boolean var1) {
      this.isSet = var1;
   }

   public final int getSymbolHash() {
      return this.symbolHash;
   }

   public void setSymbolHash(int var1) {
      this.symbolHash = var1;
   }

   public long getSessionStartTime() {
      return this.sessionStartTime;
   }

   public void setSessionStartTime(long var1) {
      this.sessionStartTime = var1;
   }

   public long getSessionEndTime() {
      return this.sessionEndTime;
   }

   public void setSessionEndTime(long var1) {
      this.sessionEndTime = var1;
   }

   public final int getConnectionHash() {
      return this.connectionHash;
   }

   public void setConnectionHash(int var1) {
      this.connectionHash = var1;
   }

   public double getAsk() {
      return this.ask;
   }

   public void setAsk(double var1) {
      this.ask = var1;
   }

   public double getBid() {
      return this.bid;
   }

   public void setBid(double var1) {
      this.bid = var1;
   }

   public double getVolume() {
      return this.volume;
   }

   public void copyValues(TickEvent var1) {
      this.time = var1.time;
      this.symbolHash = var1.symbolHash;
      this.connectionHash = var1.connectionHash;
      this.bid = var1.bid;
      this.ask = var1.ask;
      this.volume = var1.volume;
      this.sessionStartTime = var1.sessionStartTime;
      this.sessionEndTime = var1.sessionEndTime;
      this.barClose = var1.barClose;
      this.barClose = var1.barClose;
   }

   public long getTime() {
      return this.time;
   }

   public void setTime(long var1) {
      this.time = var1;
   }

   public void setVolume(double var1) {
      this.volume = var1;
   }

   public void set(long var1, int var3, int var4, double var5, double var7, double var9, long var11, long var13, boolean var15, boolean var16) {
      this.time = var1;
      this.symbolHash = var4;
      this.connectionHash = var3;
      this.bid = var5;
      this.ask = var7;
      this.volume = var9;
      this.sessionStartTime = var11;
      this.sessionEndTime = var13;
      this.barOpen = var15;
      this.barClose = var16;
   }

   public void set(long var1, int var3, int var4, double var5, double var7, double var9, long var11, boolean var13, boolean var14) {
      this.time = var1;
      this.symbolHash = var4;
      this.connectionHash = var3;
      this.bid = var5;
      this.ask = var7;
      this.volume = var9;
      this.sessionStartTime = var11;
      this.barOpen = var13;
      this.barClose = var14;
   }

   public void set(TickEvent var1) {
      this.set(
         var1.time,
         var1.connectionHash,
         var1.symbolHash,
         var1.bid,
         var1.ask,
         var1.volume,
         var1.sessionStartTime,
         var1.sessionEndTime,
         var1.barOpen,
         var1.barClose
      );
   }

   public boolean isBarOpen() {
      return this.barOpen;
   }

   public boolean isBarClose() {
      return this.barClose;
   }

   @Override
   public String toString() {
      StringBuffer var1 = new StringBuffer();
      var1.append(this.ask);
      var1.append(",");
      var1.append(this.bid);
      return var1.toString();
   }
}
