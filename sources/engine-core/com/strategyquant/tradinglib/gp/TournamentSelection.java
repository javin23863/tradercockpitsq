package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TournamentSelection<T extends IGPNode> implements ISelectionStrategy<T> {
   public static final Logger Log = LoggerFactory.getLogger("TournamentSelection");
   private int tournamentSize = 3;
   private double tournamentProbability = 0.8;
   private GPFitnessComparator gpComparator = new GPFitnessComparator(false, (byte)11);

   @Override
   public <T extends IGPNode> List<T> select(List<T> var1, boolean var2, int var3, IRandomGenerator var4, List<T> var5) {
      if (!var2) {
         throw new IllegalArgumentException("Not implemented for non-natural fitness");
      } else {
         return this.select2(var1, var2, var3, var4, var5);
      }
   }

   public <T extends IGPNode> List<T> select2(List<T> var1, boolean var2, int var3, IRandomGenerator var4, List<T> var5) {
      ObjectArrayList var6 = new ObjectArrayList();
      int var7 = (int)(var3 * 0.2);
      if (var7 > 15) {
         var7 = 10;
      }

      if (var7 > 20) {
         var7 /= 2;
      }

      ArrayList var8 = new ArrayList(var3);

      for (int var9 = 0; var9 < var3; var9++) {
         IGPNode var10 = this.select(var1, var6, var4);
         var8.add(var10);
         int var11 = 0;

         for (int var12 = 0; var12 < var8.size(); var12++) {
            IGPNode var13 = (IGPNode)var8.get(var12);
            if (var13.getGPIDs().isSame(var10.getGPIDs())) {
               var11++;
            }
         }

         int var15 = 0;

         for (int var16 = 0; var16 < var8.size(); var16++) {
            IGPNode var14 = (IGPNode)var8.get(var16);
            if (var14.getGPIDs().isSame(var10.getGPIDs())) {
               var15++;
            }
         }

         if (var11 + var15 > var7) {
            this.removeSameFromPopulation(var10.getGPIDs(), var1);
         }
      }

      return var8;
   }

   private <T extends IGPNode> T removeSameFromPopulation(GPIDs var1, List<T> var2) {
      for (int var3 = 0; var3 < var2.size(); var3++) {
         if (var1.isSame(((IGPNode)var2.get(var3)).getGPIDs()) && var2.size() > 2) {
            var2.remove(var3);
            var3--;
         }
      }

      return null;
   }

   private <T extends IGPNode> T select(List<T> var1, ObjectArrayList<T> var2, IRandomGenerator var3) {
      int var4 = var1.size();
      var2.clear();

      for (int var5 = 0; var5 < this.tournamentSize; var5++) {
         var2.add((IGPNode)var1.get(var3.nextInt(var4)));
      }

      Collections.sort(var2, this.gpComparator);

      for (int var8 = 1; var8 <= this.tournamentSize; var8++) {
         double var6 = var3.nextDouble();
         if (var6 < this.tournamentProbability) {
            return (T)var2.get(this.tournamentSize - var8);
         }
      }

      return (T)var2.get(this.tournamentSize - 1);
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
