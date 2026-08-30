package com.strategyquant.tradinglib.optimization;

public class OptimizationMethods {
   public static final int Automatic = 0;
   public static final int BruteForce = 1;
   public static final int GeneticOptimization = 2;

   public static PopulationGenerations suggestPopulationGenerations(long var0, int var2, int var3) {
      PopulationGenerations var4 = new PopulationGenerations();
      var4.method = var2;
      if (var3 == 1000000001) {
         var4.method = 1;
         return var4;
      }

      if (var3 < 0) {
         var3 = 10000;
      }

      if (!(var0 <= var3 * 1.3) || var4.method != 1 && var4.method != 0) {
         var4.generations = 10;
         var4.method = 2;
         long var5 = Math.min(var0, var3);
         var4.population = (int)(1L * (var5 / var4.generations));
         if (var4.population > 500) {
            var4.generations = (int)(var4.generations * Math.ceil(var4.population / 500));
            var4.population = 500;
         }

         if (var4.population < 1000) {
            var4.population = var4.population - var4.population % 10;
         }

         if (var4.population == 0) {
            var4.population = 10;
         }

         if (var4.generations == 0) {
            var4.generations = 3;
         }

         return var4;
      } else {
         var4.method = 1;
         return var4;
      }
   }

   public static long getTotalCombinations(long var0, PopulationGenerations var2, int var3) {
      if (var3 > 0) {
         return var3;
      } else {
         return var2.method == 1 ? var0 : (long)var2.population * var2.generations;
      }
   }
}
