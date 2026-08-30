package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import java.util.ArrayList;
import java.util.List;

public class OptimizationIndexesMutation implements IEvolutionaryOperator<OptimizationIndexes> {
   private OptimizationParams parameters;
   private final double mutationProbability;

   public OptimizationIndexesMutation(OptimizationParams var1, double var2) {
      this.parameters = var1;
      this.mutationProbability = var2;
   }

   @Override
   public List<OptimizationIndexes> apply(List<OptimizationIndexes> var1, IRandomGenerator var2, AbstractFactory<OptimizationIndexes> var3, int var4, int var5) throws Exception {
      ArrayList var6 = new ArrayList(var1.size());

      for (int var7 = 0; var7 < var1.size(); var7++) {
         OptimizationIndexes var8 = (OptimizationIndexes)var1.get(var7);
         short[] var9 = var8.getIndexes();
         short[] var10 = this.mutate(var9, var2);
         if (var10 != null) {
            OptimizationIndexes var11 = new OptimizationIndexes(var10);
            GPIDs var12 = var8.getGPIDs();
            GPIDs var13 = new GPIDs();
            var13.islandIndex = var4;
            var13.generationIndex = var5;
            var13.generationType = "Mutation";
            var13.parent1 = var12 != null ? var12.toShortString() : "";
            var11.setGPIDs(var13);
            var6.add(var11);
         } else {
            var6.add(var8);
         }
      }

      return var6;
   }

   private short[] mutate(short[] var1, IRandomGenerator var2) throws Exception {
      short[] var3 = new short[var1.length];
      if (var1 == null) {
         return null;
      }

      boolean var4 = false;

      for (int var5 = 0; var5 < var1.length; var5++) {
         if (var2.probability(this.mutationProbability)) {
            var3[var5] = (short)var2.nextInt(this.parameters.get(var5).getCombinationsCount());
            var4 = true;
         } else {
            var3[var5] = var1[var5];
         }
      }

      return var4 ? var3 : null;
   }
}
