package com.strategyquant.tradinglib.stockchart;

public class Trade {
   private boolean shortTrade;
   private long id;
   private long index;
   private long openTime;
   private long closeTime;
   private long realOpenTime;
   private long realCloseTime;
   private double openValue;
   private double closeValue;
   private double SLValue;
   private double PTValue;
   private long openId;
   private long closeId;

   public boolean isShortTrade() {
      return this.shortTrade;
   }

   public void setShortTrade(boolean var1) {
      this.shortTrade = var1;
   }

   public long getOpenTime() {
      return this.openTime;
   }

   public void setOpenTime(long var1) {
      this.openTime = var1;
   }

   public long getCloseTime() {
      return this.closeTime;
   }

   public void setCloseTime(long var1) {
      this.closeTime = var1;
   }

   public double getOpenValue() {
      return this.openValue;
   }

   public void setOpenValue(double var1) {
      this.openValue = var1;
   }

   public double getCloseValue() {
      return this.closeValue;
   }

   public void setCloseValue(double var1) {
      this.closeValue = var1;
   }

   public long getOpenId() {
      return this.openId;
   }

   public void setOpenId(long var1) {
      this.openId = var1;
   }

   public long getCloseId() {
      return this.closeId;
   }

   public void setCloseId(long var1) {
      this.closeId = var1;
   }

   public long getId() {
      return this.id;
   }

   public void setId(long var1) {
      this.id = var1;
   }

   public long getRealOpenTime() {
      return this.realOpenTime;
   }

   public void setRealOpenTime(long var1) {
      this.realOpenTime = var1;
   }

   public long getRealCloseTime() {
      return this.realCloseTime;
   }

   public void setRealCloseTime(long var1) {
      this.realCloseTime = var1;
   }

   public long getIndex() {
      return this.index;
   }

   public void setIndex(long var1) {
      this.index = var1;
   }

   public double getSLValue() {
      return this.SLValue;
   }

   public void setSLValue(double var1) {
      this.SLValue = var1;
   }

   public double getPTValue() {
      return this.PTValue;
   }

   public void setPTValue(double var1) {
      this.PTValue = var1;
   }
}
