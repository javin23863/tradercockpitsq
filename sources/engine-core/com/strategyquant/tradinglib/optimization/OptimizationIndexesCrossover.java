package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.gp.AbstractCrossover;
import com.strategyquant.tradinglib.gp.GPIDs;
import java.util.ArrayList;
import java.util.List;

public class OptimizationIndexesCrossover extends AbstractCrossover<OptimizationIndexes> {
   private OptimizationParams parameters;

   public OptimizationIndexesCrossover(OptimizationParams var1) {
      this(var1, 1);
   }

   public OptimizationIndexesCrossover(OptimizationParams var1, int var2) {
      super(var2);
      this.parameters = var1;
   }

   protected List<OptimizationIndexes> mate(OptimizationIndexes var1, OptimizationIndexes var2, int var3, IRandomGenerator var4, int var5, int var6) {
      short[] var7 = var1.getIndexes();
      short[] var8 = var1.getIndexes();
      if (var7.length != var8.length) {
         throw new IllegalArgumentException(L.tsq("Cannot perform cross-over with different length parents."));
      }

      int var11 = 10;

      short[] var9;
      short[] var10;
      do {
         var11--;
         var9 = this.copyParent(var7);
         var10 = this.copyParent(var8);
         if (var11 <= 0) {
            break;
         }

         for (int var12 = 0; var12 < var3; var12++) {
            int var13 = 0;
            int var14 = 0;
            if (var7.length > 1) {
               var13 = var4.nextInt(var7.length - 1);
               var14 = var13 + var4.nextInt(var7.length - var13);
            }

            for (int var15 = var13; var15 <= var14; var15++) {
               short var16 = var9[var15];
               var9[var15] = var10[var15];
               var10[var15] = var16;
            }
         }
      } while (!this.checkCorrect(var9) || !this.checkCorrect(var10));

      ArrayList var17 = new ArrayList(2);
      var17.add(this.checkIsSameAndAdd(var1, var2, var9, var5, var6));
      var17.add(this.checkIsSameAndAdd(var2, var1, var10, var5, var6));
      return var17;
   }

   private OptimizationIndexes checkIsSameAndAdd(OptimizationIndexes var1, OptimizationIndexes var2, short[] var3, int var4, int var5) {
      if (var1.isSame(var3)) {
         return var1;
      }

      OptimizationIndexes var6 = new OptimizationIndexes(var3);
      GPIDs var7 = new GPIDs();
      var7.islandIndex = var4;
      var7.generationIndex = var5;
      var7.generationType = "Crossover";
      var7.parent1 = var1.getGPIDs().toShortString();
      var7.parent2 = var2.getGPIDs().toShortString();
      var6.setGPIDs(var7);
      return var6;
   }

   private boolean checkCorrect(short[] var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < this.parameters.size(); var3++) {
         Parameter var4 = this.parameters.get(var3);
         if (var4.isUsed()) {
            if (var1[var2] >= var4.getCombinationsCount()) {
               return false;
            }

            var2++;
         }
      }

      return true;
   }

   private short[] copyParent(short[] var1) {
      short[] var2 = new short[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3];
      }

      return var2;
   }
}
