package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.HashMap;

public class PortfolioFitnessCache {
   private static final int MAX_CACHE_SIZE = 100000;
   private HashMap<Integer, Double> cache = new HashMap<>();

   public void reset() {
      this.cache.clear();
   }

   public synchronized double getFitness(int var1) {
      return this.cache.containsKey(var1) ? this.cache.get(var1) : -1.0;
   }

   public synchronized void setFitness(int var1, double var2) {
      if (!this.cache.containsKey(var1)) {
         if (this.cache.size() > 100000) {
            this.cache.clear();
         }

         this.cache.put(var1, var2);
      }
   }
}
