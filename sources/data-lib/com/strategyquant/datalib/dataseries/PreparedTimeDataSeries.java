package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.TradingException;

public class PreparedTimeDataSeries extends TimeDataSeries {
   private int currentRealIndex = -1;
   private long currentValue;

   public PreparedTimeDataSeries(TimeDataSeries var1, int var2) {
      super(0);
      this.longValues = var1.longValues;
      this.dataSeriesName = var1.dataSeriesName;
      this.setIndyStartingBar(var2);
   }

   @Override
   public void addChangeListener(int var1, IDataSeriesChangeListener var2) {
      super.addChangeListener(var1, var2);
   }

   @Override
   public long get(int var1) throws TradingException {
      return this.getRealIndex(var1) == this.currentRealIndex ? this.currentValue : super._getLong(var1);
   }

   @Override
   public void set(int var1, long var2) throws TradingException {
      this.set(var1, var2, false);
   }

   public void set(int var1, long var2, boolean var4) throws TradingException {
      int var5 = this.getRealIndex(var1);
      int var6 = this.getShift();
      this.currentRealIndex = var5;
      this.currentValue = var2;
      if (this.hasLinkedDataComputer() && (this.computedUntil == var1 + var6 + 1 || this.computedUntil == -1 && var1 == this.size() - 1)) {
         this.computedUntil = var1;
      }

      if (this.onBarChangeListeners != null && !var4) {
         this.callOnBarChangeListeners(var1 + var6, this.size() + var6);
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 + var6, this.size() + var6);
      }
   }

   public void set(long var1) throws TradingException {
      this.set(0, var1, false);
   }

   @Override
   public void addValues(int var1) throws TradingException {
      throw new TradingException("Not implemented here!");
   }

   @Override
   public void add(long var1) throws TradingException {
      throw new TradingException("Not implemented here!");
   }

   public void addValuesBefore(int var1) throws Exception {
      throw new TradingException("Not implemented here!");
   }

   public void addValuesBefore(int var1, long var2) throws Exception {
      throw new TradingException("Not implemented here!");
   }

   @Override
   public void destroy() {
   }
}
