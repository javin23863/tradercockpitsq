package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.utils.ISQCloneable;

public class GPIDs implements ISQCloneable<GPIDs> {
   public int islandIndex = -1;
   public int generationIndex = -1;
   public int nodeIndex = -1;
   public String generationType = GPGenerationTypes.Unknown;
   public String parent1 = null;
   public String parent2 = null;

   public GPIDs() {
   }

   public GPIDs(GPIDs var1) {
      this.set(var1);
   }

   @Override
   public String toString() {
      if (this.generationType.equals("Mutation")) {
         return String.format("%d.%d.%d (%s from %s)", this.islandIndex + 1, this.generationIndex, this.nodeIndex, this.generationType, this.parent1);
      } else {
         return this.generationType.equals("Crossover")
            ? String.format(
               "%d.%d.%d (%s from %s+%s)", this.islandIndex + 1, this.generationIndex, this.nodeIndex, this.generationType, this.parent1, this.parent2
            )
            : String.format("%d.%d.%d (%s)", this.islandIndex + 1, this.generationIndex, this.nodeIndex, this.generationType);
      }
   }

   public String toShortString() {
      return String.format("%d.%d.%d", this.islandIndex + 1, this.generationIndex, this.nodeIndex);
   }

   public void set(GPIDs var1) {
      this.islandIndex = var1.islandIndex;
      this.generationIndex = var1.generationIndex;
      this.nodeIndex = var1.nodeIndex;
      this.generationType = var1.generationType;
      this.parent1 = var1.parent1;
      this.parent2 = var1.parent2;
   }

   public GPIDs getClone() throws Exception {
      GPIDs var1 = new GPIDs();
      var1.islandIndex = this.islandIndex;
      var1.generationIndex = this.generationIndex;
      var1.nodeIndex = this.nodeIndex;
      var1.generationType = this.generationType;
      var1.parent1 = this.parent1;
      var1.parent2 = this.parent2;
      return var1;
   }

   public boolean isSame(GPIDs var1) {
      return var1 == null ? false : this.islandIndex == var1.islandIndex && this.generationIndex == var1.generationIndex && this.nodeIndex == var1.nodeIndex;
   }
}
