package com.strategyquant.tradinglib.gp;

import java.util.Comparator;

public class GPFitnessComparator<T extends IGPNode> implements Comparator<T> {
   private boolean natural;
   private byte sampleType;

   public GPFitnessComparator(boolean var1, byte var2) {
      this.natural = var1;
      this.sampleType = var2;
   }

   public int compare(T var1, T var2) {
      if (var1 == null || var2 == null) {
         return 0;
      } else {
         return this.natural
            ? Double.compare(var2.getFitness(this.sampleType), var1.getFitness(this.sampleType))
            : -1 * Double.compare(var2.getFitness(this.sampleType), var1.getFitness(this.sampleType));
      }
   }
}
