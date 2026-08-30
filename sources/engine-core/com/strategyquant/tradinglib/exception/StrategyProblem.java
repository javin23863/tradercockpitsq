package com.strategyquant.tradinglib.exception;

public class StrategyProblem {
   public int code;
   public String shortInfo;
   public String longInfo;

   public StrategyProblem(int var1, String var2, String var3) {
      this.code = var1;
      this.shortInfo = var2;
      this.longInfo = var3;
   }
}
