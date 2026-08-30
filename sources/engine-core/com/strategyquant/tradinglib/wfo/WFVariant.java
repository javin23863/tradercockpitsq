package com.strategyquant.tradinglib.wfo;

public class WFVariant {
   public String params;
   public short[] indexes;
   public int simulationOLHash;
   public String paramsFile;

   public WFVariant(String var1, short[] var2, String var3, int var4) {
      this.params = var1;
      this.indexes = var2;
      this.paramsFile = var3;
      this.simulationOLHash = var4;
   }
}
