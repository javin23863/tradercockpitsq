package com.strategyquant.tradinglib.engine.stockpicker.data.symbols;

import com.strategyquant.datalib.TradingException;

public class LoadedSymbolData {
   public float[] OpenD;
   public float[] HighD;
   public float[] LowD;
   public float[] CloseD;
   public float[] VolumeD;
   public float[] ValueD;
   public float[] OpenW;
   public float[] HighW;
   public float[] LowW;
   public float[] CloseW;
   public float[] VolumeW;
   public float[] OpenM;
   public float[] HighM;
   public float[] LowM;
   public float[] CloseM;
   public float[] VolumeM;
   private int realDataIndexStartD = -1;
   private int realDataIndexEndD = -1;
   private int realDataIndexStartW = -1;
   private int realDataIndexEndW = -1;
   private int realDataIndexStartM = -1;
   private int realDataIndexEndM = -1;
   private Exception loadException = null;

   public LoadedSymbolData() {
   }

   public LoadedSymbolData(int var1, int var2, int var3) {
      this.OpenD = new float[var1];
      this.HighD = new float[var1];
      this.LowD = new float[var1];
      this.CloseD = new float[var1];
      this.VolumeD = new float[var1];
      this.ValueD = new float[var1];
      this.OpenW = new float[var2];
      this.HighW = new float[var2];
      this.LowW = new float[var2];
      this.CloseW = new float[var2];
      this.VolumeW = new float[var2];
      this.OpenM = new float[var3];
      this.HighM = new float[var3];
      this.LowM = new float[var3];
      this.CloseM = new float[var3];
      this.VolumeM = new float[var3];
   }

   public float OpenD(int var1) {
      return this.OpenD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.OpenD[var1] : 0.0F;
   }

   public float OpenW(int var1) {
      return this.OpenW != null && var1 >= this.realDataIndexStartW && var1 <= this.realDataIndexEndW ? this.OpenW[var1] : 0.0F;
   }

   public float OpenM(int var1) {
      return this.OpenM != null && var1 >= this.realDataIndexStartM && var1 <= this.realDataIndexEndM ? this.OpenM[var1] : 0.0F;
   }

   public float CloseD(int var1) {
      return this.CloseD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.CloseD[var1] : 0.0F;
   }

   public float CloseW(int var1) {
      return this.CloseW != null && var1 >= this.realDataIndexStartW && var1 <= this.realDataIndexEndW ? this.CloseW[var1] : 0.0F;
   }

   public float CloseM(int var1) {
      return this.CloseM != null && var1 >= this.realDataIndexStartM && var1 <= this.realDataIndexEndM ? this.CloseM[var1] : 0.0F;
   }

   public float HighD(int var1) {
      return this.HighD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.HighD[var1] : 0.0F;
   }

   public float HighW(int var1) {
      return this.HighW != null && var1 >= this.realDataIndexStartW && var1 <= this.realDataIndexEndW ? this.HighW[var1] : 0.0F;
   }

   public float HighM(int var1) {
      return this.HighM != null && var1 >= this.realDataIndexStartM && var1 <= this.realDataIndexEndM ? this.HighM[var1] : 0.0F;
   }

   public float LowD(int var1) {
      return this.LowD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.LowD[var1] : 0.0F;
   }

   public float LowW(int var1) {
      return this.LowW != null && var1 >= this.realDataIndexStartW && var1 <= this.realDataIndexEndW ? this.LowW[var1] : 0.0F;
   }

   public float LowM(int var1) {
      return this.LowM != null && var1 >= this.realDataIndexStartM && var1 <= this.realDataIndexEndM ? this.LowM[var1] : 0.0F;
   }

   public float VolumeD(int var1) {
      return this.VolumeD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.VolumeD[var1] : 0.0F;
   }

   public float VolumeW(int var1) {
      return this.VolumeW != null && var1 >= this.realDataIndexStartW && var1 <= this.realDataIndexEndW ? this.VolumeW[var1] : 0.0F;
   }

   public float VolumeM(int var1) {
      return this.VolumeM != null && var1 >= this.realDataIndexStartM && var1 <= this.realDataIndexEndM ? this.VolumeM[var1] : 0.0F;
   }

   public void setRealDataIndexStartD(int var1) throws TradingException {
      if (var1 < 0) {
         throw new TradingException(String.format("Cannot set START index of real data  - Index out of range %d, starts from 0.", var1, 0));
      }

      this.realDataIndexStartD = var1;
   }

   public void setRealDataIndexEndD(int var1) throws TradingException {
      if (var1 >= this.OpenD.length) {
         throw new TradingException(String.format("Cannot set END index of real data  - Index out of range %d, size %d.", var1, this.OpenD.length));
      }

      this.realDataIndexEndD = var1;
   }

   public void setRealDataIndexStartW(int var1) {
      this.realDataIndexStartW = var1;
   }

   public void setRealDataIndexEndW(int var1) {
      this.realDataIndexEndW = var1;
   }

   public void setRealDataIndexStartM(int var1) {
      this.realDataIndexStartM = var1;
   }

   public void setRealDataIndexEndM(int var1) {
      this.realDataIndexEndM = var1;
   }

   public int getRealDataIndexStartD() {
      return this.realDataIndexStartD;
   }

   public int getRealDataIndexEndD() {
      return this.realDataIndexEndD;
   }

   public int getRealDataIndexStartW() {
      return this.realDataIndexStartW;
   }

   public int getRealDataIndexEndW() {
      return this.realDataIndexEndW;
   }

   public int getRealDataIndexStartM() {
      return this.realDataIndexStartM;
   }

   public int getRealDataIndexEndM() {
      return this.realDataIndexEndM;
   }

   public boolean dataExistsD(int var1) {
      return this.OpenD != null && var1 >= this.realDataIndexStartD && var1 <= this.realDataIndexEndD ? this.OpenD[var1] > 0.0F : false;
   }

   public Exception getException() {
      return this.loadException;
   }

   public void setException(Exception var1) {
      this.loadException = var1;
   }

   public int getRealDataIndexStart(String var1) throws TradingException {
      switch (var1) {
         case "D1":
            return this.getRealDataIndexStartD();
         case "Weekly":
            return this.getRealDataIndexStartW();
         case "Monthly":
            return this.getRealDataIndexStartM();
         default:
            throw new TradingException(String.format("Cannot get data START index - unexpected timeframe %s.", var1));
      }
   }

   public int getRealDataIndexEnd(String var1) throws TradingException {
      switch (var1) {
         case "D1":
            return this.getRealDataIndexEndD();
         case "Weekly":
            return this.getRealDataIndexEndW();
         case "Monthly":
            return this.getRealDataIndexEndM();
         default:
            throw new TradingException(String.format("Cannot get data END index - unexpected timeframe %s.", var1));
      }
   }

   public LoadedSymbolData clone() {
      LoadedSymbolData var1 = new LoadedSymbolData();
      var1.OpenD = this.OpenD != null ? (float[])this.OpenD.clone() : null;
      var1.HighD = this.HighD != null ? (float[])this.HighD.clone() : null;
      var1.LowD = this.LowD != null ? (float[])this.LowD.clone() : null;
      var1.CloseD = this.CloseD != null ? (float[])this.CloseD.clone() : null;
      var1.VolumeD = this.VolumeD != null ? (float[])this.VolumeD.clone() : null;
      var1.ValueD = this.ValueD != null ? (float[])this.ValueD.clone() : null;
      var1.OpenW = this.OpenW != null ? (float[])this.OpenW.clone() : null;
      var1.HighW = this.HighW != null ? (float[])this.HighW.clone() : null;
      var1.LowW = this.LowW != null ? (float[])this.LowW.clone() : null;
      var1.CloseW = this.CloseW != null ? (float[])this.CloseW.clone() : null;
      var1.VolumeW = this.VolumeW != null ? (float[])this.VolumeW.clone() : null;
      var1.OpenM = this.OpenM != null ? (float[])this.OpenM.clone() : null;
      var1.HighM = this.HighM != null ? (float[])this.HighM.clone() : null;
      var1.LowM = this.LowM != null ? (float[])this.LowM.clone() : null;
      var1.CloseM = this.CloseM != null ? (float[])this.CloseM.clone() : null;
      var1.VolumeM = this.VolumeM != null ? (float[])this.VolumeM.clone() : null;
      var1.realDataIndexStartD = this.realDataIndexStartD;
      var1.realDataIndexEndD = this.realDataIndexEndD;
      var1.realDataIndexStartW = this.realDataIndexStartW;
      var1.realDataIndexEndW = this.realDataIndexEndW;
      var1.realDataIndexStartM = this.realDataIndexStartM;
      var1.realDataIndexEndM = this.realDataIndexEndM;
      var1.loadException = this.loadException;
      return var1;
   }
}
