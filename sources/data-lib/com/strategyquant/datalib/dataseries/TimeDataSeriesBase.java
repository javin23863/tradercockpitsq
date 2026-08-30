package com.strategyquant.datalib.dataseries;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TradingException;

public abstract class TimeDataSeriesBase extends DataSeriesBase {
   protected ILongValuesList longValues = null;

   public TimeDataSeriesBase() {
      this(10000, 10000, null);
   }

   public TimeDataSeriesBase(int var1) {
      this(var1, 10000, null);
   }

   public TimeDataSeriesBase(int var1, String var2) {
      this(var1, 10000, var2);
   }

   public TimeDataSeriesBase(int var1, String var2, ChartDef var3) {
      super(0, var2, var3);
      this.allocationSize = var1;
      this.growStep = 10000;
   }

   public TimeDataSeriesBase(int var1, int var2) {
      this(var1, var2, null);
   }

   public TimeDataSeriesBase(int var1, int var2, String var3) {
      super(0, var3);
      this.allocationSize = var1;
      this.growStep = var2;
   }

   protected synchronized void checkValuesInitialized() {
      if (this.longValues == null && this.allocationSize != 0) {
         this.longValues = LongListCache.get(this.allocationSize);
      }
   }

   protected long _getLong(int var1) throws TradingException {
      this.checkValuesInitialized();
      if (var1 >= this.size()) {
         return 0L;
      }

      int var2 = this.getRealIndex(var1);
      this.computePotentialMissingValues(var1);
      return var2 == this.longValues.size() ? 0L : this.longValues.getLong(var2);
   }

   protected void _setLong(int var1, long var2) throws TradingException {
      this.checkValuesInitialized();
      if (var1 < this.size()) {
         int var4 = this.getRealIndex(var1);
         this.longValues.setLong(var4, var2);
         if (this.hasLinkedDataComputer() && (this.computedUntil == var1 + this.shift + 1 || this.computedUntil == -1 && var1 == this.size() - 1)) {
            this.computedUntil = var1;
         }

         if (this.onBarChangeListeners != null) {
            this.callOnBarChangeListeners(var1 + this.shift, this.size() + this.shift);
         }

         if (this.onTickChangeListeners != null) {
            this.callOnTickChangeListeners(var1 + this.shift, this.size() + this.shift);
         }
      }
   }

   @Override
   public void addLongValues(int var1, long var2) throws TradingException {
      this.checkValuesInitialized();
      if (this.shift != 0) {
      }

      for (int var4 = 0; var4 < var1; var4++) {
         this.longValues.addLong(var2);
      }

      if (this.hasLinkedDataComputer() && this.computedUntil > -1) {
         this.computedUntil += var1;
      }

      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(var1 - 1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(var1 - 1, this.size());
      }
   }

   @Override
   protected void _addLong(long var1) throws TradingException {
      this.checkValuesInitialized();
      if (this.shift != 0) {
         throw new TradingException("Shift must be 0 when calling addValues() !");
      }

      this.longValues.addLong(var1);
      if (this.onBarChangeListeners != null) {
         this.callOnBarChangeListeners(1, this.size());
      }

      if (this.onTickChangeListeners != null) {
         this.callOnTickChangeListeners(1, this.size());
      }
   }

   @Override
   public void destroy() {
      if (this.longValues != null) {
         LongListCache.clear(this.longValues);
         this.longValues = null;
      }
   }

   @Override
   public int size() {
      this.checkValuesInitialized();
      return this.longValues.size() - this.shift;
   }

   @Override
   public int specialSize() {
      this.checkValuesInitialized();
      return this.longValues.size();
   }

   public long getDateBarsFromDate(long var1, int var3) {
      int var4 = -1;
      if (var1 == 0L) {
         var4 = 0;
      } else {
         var4 = this.getDFIndexSlow(var1);
      }

      if (var4 == -1) {
         return this.longValues.getLong(this.longValues.size() - 1);
      } else {
         int var5 = var4 + var3;
         if (var5 < 0) {
            return this.longValues.getLong(0);
         } else {
            return var5 >= this.longValues.size() ? this.longValues.getLong(this.longValues.size() - 1) : this.longValues.getLong(var5);
         }
      }
   }

   protected int getDFIndexEffective(long var1) {
      int var3 = -2;
      int var4 = this.longValues.size();
      if (var4 <= 0) {
         return var3;
      }

      long var5 = this.longValues.getLong(0);
      long var7 = this.longValues.getLong(var4 - 1);
      int var9 = (int)((var7 - var5) / var4);
      if (var9 == 0) {
         var9 = 1;
      }

      var9 = (int)(var9 * 1.2);
      int var10 = (int)((var1 - var5) / var9);
      if (var10 == 0) {
         var10 = 1;
      }

      for (int var11 = var10 - 1; var11 < this.longValues.size(); var11++) {
         long var12 = this.longValues.getLong(var11);
         if (var12 >= var1) {
            var3 = var11;
            break;
         }
      }

      return var3;
   }

   protected int getDFIndexSlow(long var1) {
      int var3 = -1;

      for (int var4 = 0; var4 < this.longValues.size(); var4++) {
         long var5 = this.longValues.getLong(var4);
         if (var5 >= var1) {
            var3 = var4;
            break;
         }
      }

      return var3;
   }
}
