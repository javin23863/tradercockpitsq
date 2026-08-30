package com.strategyquant.datalib;

import com.strategyquant.datalib.dataseries.DataSeriesBase;
import com.strategyquant.lib.SQUtils;

public class DataSeries extends DataSeriesBase {
   private int hashCode = -1;
   static final double p6 = 1000000.0;
   static final double factor2 = 1.0E-10;

   public DataSeries() {
   }

   public DataSeries(int var1) {
      super(var1);
   }

   public DataSeries(String var1, int var2) {
      super(var2, var1);
   }

   public DataSeries(String var1, int var2, ChartDef var3) {
      super(var2, var1, var3);
   }

   public DataSeries(int var1, int var2) {
      super(var1, var2);
   }

   public DataSeries(String var1) {
      super(100, var1);
   }

   public int chartHashCode() {
      if (this.hashCode == -1) {
         this.hashCode = this.hashCode();
      }

      return this.hashCode;
   }

   public double get(int var1) throws TradingException {
      return this._getDouble(var1);
   }

   public double getRounded(int var1) throws TradingException {
      double var2 = this._getDouble(var1);
      var2 = (var2 + 1.0E-10) * 1000000.0;
      double var4 = Math.round(var2);
      return var4 / 1000000.0;
   }

   public double getRounded(int var1, int var2) throws TradingException {
      double var3 = this._getDouble(var1);
      return SQUtils.round(var3, var2);
   }

   public void set(int var1, double var2) throws TradingException {
      this._setDouble(var1, var2, false);
   }

   public void set(int var1, double var2, boolean var4) throws TradingException {
      this._setDouble(var1, var2, var4);
   }

   public void set(double var1) throws TradingException {
      this._setDouble(0, var1, false);
   }

   public void addValues(int var1) throws TradingException {
      this.addDoubleValues(var1, 0.0);
   }

   public void add(double var1) throws TradingException {
      this._addDouble(var1);
   }
}
