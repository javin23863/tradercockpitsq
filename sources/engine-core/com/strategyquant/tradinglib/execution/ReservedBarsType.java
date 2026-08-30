package com.strategyquant.tradinglib.execution;

public class ReservedBarsType {
   public static final int LOAD_BACK = 1;
   public static final int LOAD_FROM = 2;
   public int type;
   public int minimumReservedBars;
   public int preferredReservedBars;

   public ReservedBarsType(int var1, int var2, int var3) {
      this.type = var1;
      this.minimumReservedBars = var2;
      this.preferredReservedBars = var3;
   }
}
