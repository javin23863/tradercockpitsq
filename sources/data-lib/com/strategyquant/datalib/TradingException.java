package com.strategyquant.datalib;

public class TradingException extends Exception {
   public static final int IndicatorCalculationFailed = 55;
   private int errorCode;

   public TradingException() {
   }

   public TradingException(String var1) {
      super(var1);
   }

   public TradingException(String var1, int var2) {
      super(var1);
      this.errorCode = var2;
   }

   public int getErrorCode() {
      return this.errorCode;
   }

   public TradingException(Exception var1) {
      super(var1);
   }

   public TradingException(String var1, Exception var2) {
      super(var1, var2);
   }

   public TradingException addCall(String var1) {
      return new TradingException(this.getMessage() + " - > " + var1);
   }
}
