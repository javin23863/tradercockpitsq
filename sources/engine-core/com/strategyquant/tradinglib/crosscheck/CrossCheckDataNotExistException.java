package com.strategyquant.tradinglib.crosscheck;

public class CrossCheckDataNotExistException extends Exception {
   private String crossCheckName = null;

   public CrossCheckDataNotExistException(String var1) {
      this.crossCheckName = var1;
   }
}
