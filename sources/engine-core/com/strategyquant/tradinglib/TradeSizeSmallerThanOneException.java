package com.strategyquant.tradinglib;

public class TradeSizeSmallerThanOneException extends Exception {
   public TradeSizeSmallerThanOneException(String var1) {
      super(var1);
   }

   public TradeSizeSmallerThanOneException(String var1, Exception var2) {
      super(var1, var2);
   }
}
