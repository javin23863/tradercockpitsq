package com.strategyquant.tradinglib.exchange;

public class CryptoDownloadException extends RuntimeException {
   private static final long serialVersionUID = 1L;
   private final int code;

   public CryptoDownloadException(String var1, int var2) {
      super(var1);
      this.code = var2;
   }

   public int getCode() {
      return this.code;
   }
}
