package com.strategyquant.tradinglib.optimization;

public class NotEnoughSpaceException extends Exception {
   public static final int TOO_MANY_TESTS = 1;
   public static final int NO_DISC_SPACE = 2;
   private int reason = 0;

   public NotEnoughSpaceException(int var1, String var2) {
      super(var2);
      this.reason = var1;
   }
}
