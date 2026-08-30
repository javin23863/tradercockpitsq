package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouletteWheelSelection<T extends IGPNode> implements ISelectionStrategy<T> {
   public static final Logger Log = LoggerFactory.getLogger("RouletteWheelSelection");

   @Override
   public <T extends IGPNode> List<T> select(List<T> var1, boolean var2, int var3, IRandomGenerator var4, List<T> var5) {
      double[] var6 = new double[var1.size()];
      var6[0] = this.getAdjustedFitness(((IGPNode)var1.get(0)).getFitness((byte)11), var2);

      for (int var7 = 1; var7 < var1.size(); var7++) {
         double var8 = this.getAdjustedFitness(((IGPNode)var1.get(var7)).getFitness((byte)11), var2);
         var6[var7] = var6[var7 - 1] + var8;
      }

      ArrayList var12 = new ArrayList(var3);

      for (int var13 = 0; var13 < var3; var13++) {
         double var9 = var4.nextDouble() * var6[var6.length - 1];
         int var11 = Arrays.binarySearch(var6, var9);
         if (var11 < 0) {
            var11 = Math.abs(var11 + 1);
         }

         var12.add((IGPNode)var1.get(var11));
      }

      return var12;
   }

   private double getAdjustedFitness(double var1, boolean var3) {
      if (var3) {
         return var1;
      } else {
         return var1 == 0.0 ? Double.POSITIVE_INFINITY : 1.0 / var1;
      }
   }

   private <T extends IGPNode> void printPopulation(String var1, List<T> var2) {
      Log.info("---------------------------------------------------------");
      Log.info("--- Text : " + var1);
      Log.info("---------------------------------------------------------");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         IGPNode var4 = (IGPNode)var2.get(var3);
         Log.info("Candidate " + var4.getAsString() + " - fitness: " + var4.getFitness((byte)11));
      }
   }
}
