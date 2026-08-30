package com.strategyquant.tradinglib.talib.reflection;

public class TALibTestApp {
   public static void main(String[] var0) {
      TALibFunction[] var1 = TALibFunctions.getTALibFunctions();

      for (TALibFunction var5 : var1) {
         System.out.println(var5.toString());
      }
   }
}
