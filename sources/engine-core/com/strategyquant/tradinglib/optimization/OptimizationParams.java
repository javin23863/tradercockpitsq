package com.strategyquant.tradinglib.optimization;

import java.util.ArrayList;

public class OptimizationParams extends ArrayList<Parameter> {
   public static final String TXT_MISCONFIGURATION_NOPARAMS = "Misconfiguration - no parameter was chosen to be optimized";
   ArrayList<Parameter> usedParams = null;

   public Parameter addParam(String var1, byte var2, boolean var3, double var4, double var6, double var8, double var10) {
      Parameter var12 = new Parameter(var1, var2, var3, var4, var6, var8, var10);
      this.add(var12);
      this.usedParams = null;
      return var12;
   }

   public void addParam(Parameter var1) {
      this.add(var1);
      this.usedParams = null;
   }

   public long getTotalCombinations() {
      long var1 = 1L;

      for (int var3 = 0; var3 < this.size(); var3++) {
         Parameter var4 = this.get(var3);
         if (var4.use) {
            var1 *= var4.getCombinationsCount();
         }
      }

      if (var1 <= 0L) {
         var1 = 1000000L;
      }

      return var1;
   }

   public double getValue(int var1, String var2) throws NonexistingVariableException, OptimizationException {
      for (int var3 = 0; var3 < this.size(); var3++) {
         Parameter var4 = this.get(var3);
         if (var4.getName().equals(var2)) {
            return var4.getValueByIndex(var1);
         }
      }

      throw new NonexistingVariableException(String.format("Variable '%s' doesn't exist!", var2));
   }

   public int verify() throws BadOptimizationParamsException {
      long var2 = 1L;
      int var5 = 0;

      for (int var6 = 0; var6 < this.size(); var6++) {
         Parameter var1 = this.get(var6);
         if (var1.use) {
            var5++;
         }

         if (var1.type != 2) {
            if (var1.start < var1.stop) {
               if (var1.start + var1.step < var1.start) {
                  throw new BadOptimizationParamsException(
                     String.format("Misconfiguration of values for '%s'. Start: %f, Stop: %f, Step: %f", var1.name, var1.start, var1.stop, var1.step)
                  );
               }
            } else if (var1.start + var1.step > var1.start) {
               throw new BadOptimizationParamsException(
                  String.format("Misconfiguration of values for '%s'. Start: %f, Stop: %f, Step: %f", var1.name, var1.start, var1.stop, var1.step)
               );
            }
         }

         if (var1.getCombinationsCount() > 1000) {
            throw new BadOptimizationParamsException(
               String.format("Misconfiguration (too many steps) of values for '%s'. Start: %f, Stop: %f, Step: %f", var1.name, var1.start, var1.stop, var1.step)
            );
         }

         var2 *= var1.getCombinationsCount();
      }

      if (var5 == 0) {
      }

      return var5;
   }

   public int getUsedCount() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.size(); var2++) {
         Parameter var3 = this.get(var2);
         if (var3.use) {
            var1++;
         }
      }

      return var1;
   }

   public void prepareTestParameters(long var1, short[] var3) {
      long var4 = this.getTotalCombinations();
      if (var1 >= var4) {
         var1 = var4 - 1L;
      }

      for (int var6 = 0; var6 < var3.length; var6++) {
         var3[var6] = 0;
      }

      long var12 = var1;

      for (int var8 = var3.length - 1; var8 >= 0 && var12 > 0L; var8--) {
         int var9 = this.getPositionCoef(var8);
         double var10 = (double)var12 / var9;
         if (var10 > 0.0) {
            var3[var8] = (short)var10;
         }

         var12 -= var3[var8] * var9;
      }
   }

   private int getPositionCoef(int var1) {
      int var2 = 1;
      if (this.usedParams == null) {
         this.usedParams = new ArrayList<>();

         for (int var3 = 0; var3 < this.size(); var3++) {
            if (this.get(var3).use) {
               this.usedParams.add(this.get(var3));
            }
         }
      }

      int var5 = 0;

      for (int var4 = 0; var4 < var1; var4++) {
         var2 *= this.usedParams.get(var4).getCombinationsCount();
         var5++;
      }

      return var2;
   }

   public OptimizationParams clone() {
      OptimizationParams var1 = new OptimizationParams();

      for (int var2 = 0; var2 < this.size(); var2++) {
         var1.add(this.get(var2).clone());
      }

      return var1;
   }
}
