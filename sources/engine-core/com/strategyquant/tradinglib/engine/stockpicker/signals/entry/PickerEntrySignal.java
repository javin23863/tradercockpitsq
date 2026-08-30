package com.strategyquant.tradinglib.engine.stockpicker.signals.entry;

import com.strategyquant.tradinglib.OrderTypes;

public class PickerEntrySignal {
   public String symbol;
   public byte type;
   public double price;
   public int barsValid;
   public double posScore;
   public double sl;
   public double pt;
   public int slptValidFrom;
   public int exitAfterBars;
   public byte eod = -1;
   private boolean used = true;

   public PickerEntrySignal(String var1, byte var2, double var3, int var5, double var6, double var8, int var10, int var11, byte var12) {
      this.symbol = var1;
      this.type = var2;
      this.price = var3;
      this.sl = var6;
      this.pt = var8;
      this.slptValidFrom = var10;
      this.barsValid = var5;
      this.exitAfterBars = var11;
      this.eod = var12;
      this.used = true;
   }

   public void init(String var1, byte var2, double var3, int var5, double var6, double var8, int var10, int var11, byte var12) {
      this.symbol = var1;
      this.type = var2;
      this.price = var3;
      this.sl = var6;
      this.pt = var8;
      this.slptValidFrom = var10;
      this.barsValid = var5;
      this.posScore = 0.0;
      this.exitAfterBars = var11;
      this.eod = var12;
      this.used = true;
   }

   @Override
   public String toString() {
      StringBuffer var1 = new StringBuffer(this.symbol);
      var1.append("=");
      var1.append(OrderTypes.toString(this.type));
      var1.append("(");
      var1.append(this.posScore);
      var1.append(")");
      return var1.toString();
   }

   public void setUnused() {
      this.used = false;
   }

   public boolean isUsed() {
      return this.used;
   }

   public byte getDirection() {
      return (byte)(OrderTypes.isLongOrder(this.type) ? 1 : -1);
   }
}
