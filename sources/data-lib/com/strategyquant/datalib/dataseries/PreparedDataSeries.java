package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreparedDataSeries extends DataSeries {
   public static final Logger Log = LoggerFactory.getLogger("Indicator");
   private int currentRealIndex = -1;
   private double currentValue;

   public PreparedDataSeries(DataSeries var1, int var2) {
      super(0);
      this.doubleValues = var1.doubleValues;
      this.dataSeriesName = var1.dataSeriesName;
      if (var2 > 0) {
         this.setIndyStartingBar(var2);
      } else {
         this.setIndyStartingBar(var1.getIndyStartingBar());
      }
   }

   @Override
   public void addChangeListener(int var1, IDataSeriesChangeListener var2) {
      super.addChangeListener(var1, var2);
   }

   @Override
   public double get(int var1) throws TradingException {
      return this.getRealIndex(var1) == this.currentRealIndex ? this.currentValue : super._getDouble(var1);
   }

   @Override
   public void set(int var1, double var2) throws TradingException {
      this.set(var1, var2, false);
   }

   @Override
   public void set(int var1, double var2, boolean var4) throws TradingException {
      int var5 = this.getRealIndex(var1);
      this.currentRealIndex = var5;
      this.currentValue = var2;
      if (this.hasLinkedDataComputer() && (this.computedUntil == var1 + this.shift + 1 || this.computedUntil == -1 && var1 == this.size() - 1)) {
         this.computedUntil = var1;
      }

      if (this.onBarChangeListeners != null && !var4) {
         this.callOnBarChangeListeners(var1 + this.shift, this.size() + this.shift);
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 + this.shift, this.size() + this.shift);
      }
   }

   @Override
   protected void computePotentialMissingValues(int var1) throws TradingException {
      if (this.hasLinkedDataComputer() && (this.computedUntil == -1 || var1 + this.shift <= this.computedUntil || var1 + this.shift == 0)) {
         this.linkedDataComputer.compute(this.computedUntil, var1 + this.shift);
      }
   }

   @Override
   public void set(double var1) throws TradingException {
      this.set(0, var1, false);
   }

   @Override
   public void callDataChangeListeners() throws TradingException {
      if (this.onBarChangeListeners != null) {
         int var1 = this.shift + 1;
         int var2 = this.size() + this.shift;
         this.callOnBarChangeListeners(var1, var2);
      }

      if (this.onTickChangeListeners != null) {
         int var3 = this.shift + 1;
         int var4 = this.size() + this.shift;
         this.callOnTickChangeListeners(var3, var4);
      }
   }

   @Override
   public void addValues(int var1) throws TradingException {
      throw new TradingException("Not implemented here!");
   }

   @Override
   public void add(double var1) throws TradingException {
      throw new TradingException("Not implemented here!");
   }

   public void addValuesBefore(int var1) throws Exception {
      throw new TradingException("Not implemented here!");
   }

   public void addValuesBefore(int var1, double var2) throws Exception {
      throw new TradingException("Not implemented here!");
   }

   @Override
   public void destroy() {
   }
}
