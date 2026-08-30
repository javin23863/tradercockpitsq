package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.Arrays;
import java.util.Comparator;

public class PortfolioGenome {
   private final PortfolioGenome.StrategyGene[] strategyGenes;
   private final PortfolioGenome.StrategyGenesComparator comparator = new PortfolioGenome.StrategyGenesComparator();
   private final int hash;
   private int generation;
   private double fitness;

   public PortfolioGenome(PortfolioGenome.StrategyGene[] var1) {
      Arrays.sort(var1, this.comparator);
      this.strategyGenes = var1;
      this.hash = this.computeHash();
   }

   private int computeHash() {
      int[] var1 = new int[this.strategyGenes.length];

      for (int var2 = 0; var2 < this.strategyGenes.length; var2++) {
         var1[var2] = this.strategyGenes[var2].getNumber();
      }

      return Arrays.hashCode(var1);
   }

   public PortfolioGenome.StrategyGene[] getGenes() {
      return this.strategyGenes;
   }

   @Override
   public String toString() {
      if (this.strategyGenes == null) {
         return "NULLL";
      }

      String var1 = "[";

      for (int var2 = 0; var2 < this.strategyGenes.length; var2++) {
         var1 = var1 + this.strategyGenes[var2].getNumber() + ",";
      }

      var1 = var1.substring(0, var1.length() - 1);
      return var1 + "]";
   }

   public PortfolioGenome.StrategyGene[] cloneGenes() {
      return cloneGenes(this.strategyGenes);
   }

   public static PortfolioGenome.StrategyGene[] cloneGenes(PortfolioGenome.StrategyGene[] var0) {
      PortfolioGenome.StrategyGene[] var1 = new PortfolioGenome.StrategyGene[var0.length];

      for (int var2 = 0; var2 < var0.length; var2++) {
         var1[var2] = var0[var2].clone();
      }

      return var1;
   }

   public static boolean checkPortfolioIsCorrect(PortfolioGenome.StrategyGene[] var0) {
      for (int var1 = 0; var1 < var0.length; var1++) {
         for (int var2 = var1 + 1; var2 < var0.length; var2++) {
            if (var1 != var2 && var0[var1].getNumber() == var0[var2].getNumber()) {
               return false;
            }
         }
      }

      return true;
   }

   public int getHash() {
      return this.hash;
   }

   public void setGeneration(int var1) {
      this.generation = var1;
   }

   public void setFitness(double var1) {
      this.fitness = var1;
   }

   public int getGeneration() {
      return this.generation;
   }

   public double getFitness() {
      return this.fitness;
   }

   public static int findXthAvailableGeneNumber(int var0, PortfolioGenome.StrategyGene[] var1, int var2, int var3) {
      int var4 = 0;

      for (int var5 = 0; var5 < var3; var5++) {
         if (!containsNumber(var1, var2, var5)) {
            if (var0 == var4) {
               return var5;
            }

            var4++;
         }
      }

      return -1;
   }

   private static boolean containsNumber(PortfolioGenome.StrategyGene[] var0, int var1, int var2) {
      for (int var3 = 0; var3 < var1; var3++) {
         if (var0[var3].getNumber() == var2) {
            return true;
         }
      }

      return false;
   }

   public int[] getIndexes() {
      PortfolioGenome.StrategyGene[] var1 = this.getGenes();
      int[] var2 = new int[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3].getNumber();
      }

      return var2;
   }

   public static final class StrategyGene {
      private final int number;
      private final String name;

      public StrategyGene(int var1, String var2) {
         this.number = var1;
         this.name = var2;
      }

      public StrategyGene(int var1) {
         if (var1 < 0) {
            throw new IllegalArgumentException("Cannot use -1 as argument for StrategyGene");
         }

         this.number = var1;
         this.name = "Strategy " + var1;
      }

      public int getNumber() {
         return this.number;
      }

      public String getName() {
         return this.name;
      }

      public PortfolioGenome.StrategyGene clone() {
         return new PortfolioGenome.StrategyGene(this.number, this.name);
      }
   }

   class StrategyGenesComparator implements Comparator<PortfolioGenome.StrategyGene> {
      public int compare(PortfolioGenome.StrategyGene var1, PortfolioGenome.StrategyGene var2) {
         return var1.number - var2.number;
      }
   }
}
