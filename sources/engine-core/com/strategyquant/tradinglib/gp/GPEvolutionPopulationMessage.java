package com.strategyquant.tradinglib.gp;

import java.io.Serializable;
import java.util.ArrayList;

public class GPEvolutionPopulationMessage<T extends IGPNode> implements Serializable {
   public int currentGeneration;
   public int islandIndex;
   public ArrayList<T> population;

   public GPEvolutionPopulationMessage(int var1, int var2, ArrayList<T> var3) {
      this.currentGeneration = var1;
      this.islandIndex = var2;
      this.population = var3;
   }
}
