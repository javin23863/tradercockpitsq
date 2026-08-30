package com.strategyquant.tradinglib.portfolioMaster;

import java.math.BigInteger;

public class PortfolioMasterUtils {
   public static BigInteger calculateMaxNumberOfPortfolios(int var0, int var1, int var2) {
      BigInteger var3 = BigInteger.valueOf(0L);

      for (int var4 = var1; var4 <= var2; var4++) {
         var3 = var3.add(fact(var0).divide(fact(var0 - var4).multiply(fact(var4))));
      }

      return var3;
   }

   private static BigInteger fact(int var0) {
      return factorialBig(var0);
   }

   private static BigInteger factorialBig(int var0) {
      BigInteger var1 = BigInteger.ONE;

      for (int var2 = 1; var2 <= var0; var2++) {
         var1 = var1.multiply(BigInteger.valueOf(var2));
      }

      return var1;
   }
}
