package com.strategyquant.tradinglib.backtest;

public class HashPair {
   public int connectionHash;
   public int symbolHash;

   public HashPair(int var1, int var2) {
      this.connectionHash = var1;
      this.symbolHash = var2;
   }

   public HashPair() {
   }
}
