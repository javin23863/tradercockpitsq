package com.strategyquant.tradinglib.gp;

public class GPFitnessEvolutionData {
   public FitnessCollectionData[] generationData;
   public int islandIndex;
   public int generation = 0;
   public int populationSize = 0;

   public GPFitnessEvolutionData(int var1, int var2) {
      this.islandIndex = var1;
      this.generationData = new FitnessCollectionData[var2 + 2];
   }

   public void reset() {
      this.generation = 0;
   }
}
