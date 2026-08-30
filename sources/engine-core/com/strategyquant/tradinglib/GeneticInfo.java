package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.gp.GPEvolutionMessage;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticInfo {
   private static final Logger Log = LoggerFactory.getLogger(GeneticInfo.class);
   private boolean geneticBuild;
   private GPFitnessEvolutionData[] islandsEvoData;

   public boolean isGeneticBuild() {
      return this.geneticBuild;
   }

   public GPFitnessEvolutionData[] getIslandsEvoData() {
      return this.islandsEvoData;
   }

   public void reset(boolean var1, int var2) {
      this.geneticBuild = var1;
      if (this.geneticBuild) {
         this.islandsEvoData = new GPFitnessEvolutionData[var2];
      }
   }

   public void update(GPEvolutionMessage var1) {
      if (var1.islandIndex >= 0 && var1.islandIndex < this.islandsEvoData.length) {
         this.islandsEvoData[var1.islandIndex] = var1.fitnessEvolutionData;
      }
   }
}
