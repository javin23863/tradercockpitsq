package com.strategyquant.tradinglib.exchange;

public class SymbolInfo {
   private double pointValue = 1.0;
   private double tickSize = 1.0E-8;
   private double tickStep = 1.0E-8;
   private double defaultSpread = 0.0;
   private double defaultSlippage = 0.0;
   private long dateFrom;

   public double getPointValue() {
      return this.pointValue;
   }

   public void setPointValue(double var1) {
      this.pointValue = var1;
   }

   public double getTickSize() {
      return this.tickSize;
   }

   public void setTickSize(double var1) {
      this.tickSize = var1;
   }

   public double getTickStep() {
      return this.tickStep;
   }

   public void setTickStep(double var1) {
      this.tickStep = var1;
   }

   public double getDefaultSpread() {
      return this.defaultSpread;
   }

   public void setDefaultSpread(double var1) {
      this.defaultSpread = var1;
   }

   public double getDefaultSlippage() {
      return this.defaultSlippage;
   }

   public void setDefaultSlippage(double var1) {
      this.defaultSlippage = var1;
   }

   public long getDateFrom() {
      return this.dateFrom;
   }

   public void setDateFrom(long var1) {
      this.dateFrom = var1;
   }
}
