package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;

public class PortfolioGenomeCrossover extends AbstractCrossover<PortfolioGenome> {
   private GenomeSettings settings;

   public PortfolioGenomeCrossover(GenomeSettings var1) {
      super(var1.crossoverPoints, var1.crossoverProbability);
      this.settings = var1;
   }

   protected List<PortfolioGenome> mate(PortfolioGenome var1, PortfolioGenome var2, int var3, Random var4) {
      for (int var6 = 0; var6 < 1000; var6++) {
         List var5 = this.mateOnce(var1, var2, var3, var4);
         if (var5 != null) {
            return var5;
         }
      }

      ArrayList var7 = new ArrayList(2);
      var7.add(var1);
      var7.add(var2);
      return var7;
   }

   private List<PortfolioGenome> mateOnce(PortfolioGenome var1, PortfolioGenome var2, int var3, Random var4) {
      PortfolioGenome.StrategyGene[] var5 = this.copyParent(var1);
      PortfolioGenome.StrategyGene[] var6 = this.copyParent(var2);

      for (int var7 = 0; var7 < var3; var7++) {
         int var8 = var4.nextInt(var5.length);
         int var9 = 1 + var4.nextInt(var5.length);
         PortfolioGenome.StrategyGene[] var10 = this.copyParent(var1);
         this.copyGenes(var5, var10, var8, var9);
         this.copyGenes(var6, var5, var8, var9);
         this.copyGenes(var10, var6, var8, var9);
      }

      var5 = this.fixGenome(var5);
      var6 = this.fixGenome(var6);
      if (!PortfolioGenome.checkPortfolioIsCorrect(var5)) {
         return null;
      }

      if (!PortfolioGenome.checkPortfolioIsCorrect(var6)) {
         return null;
      }

      PortfolioGenome var13 = new PortfolioGenome(var5);
      PortfolioGenome var14 = new PortfolioGenome(var6);
      ArrayList var15 = new ArrayList(2);
      var15.add(var13);
      var15.add(var14);
      return var15;
   }

   private PortfolioGenome.StrategyGene[] fixGenome(PortfolioGenome.StrategyGene[] var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < var1.length; var3++) {
         if (var1[var3] != null) {
            var2++;
         }
      }

      PortfolioGenome.StrategyGene[] var6 = new PortfolioGenome.StrategyGene[var2];
      int var4 = 0;
      int var5 = 0;

      while (var4 < var1.length) {
         if (var1[var4] != null) {
            var6[var5++] = var1[var4];
         }

         var4++;
      }

      return var6;
   }

   private void copyGenes(PortfolioGenome.StrategyGene[] var1, PortfolioGenome.StrategyGene[] var2, int var3, int var4) {
      for (int var5 = var3; var5 < var3 + var4; var5++) {
         if (var5 < var2.length && var1[var5] != null) {
            var2[var5] = var1[var5].clone();
         }
      }
   }

   private PortfolioGenome.StrategyGene[] copyParent(PortfolioGenome var1) {
      PortfolioGenome.StrategyGene[] var2 = new PortfolioGenome.StrategyGene[this.settings.maxStrategiesInPortfolio];
      PortfolioGenome.StrategyGene[] var3 = var1.getGenes();

      for (int var4 = 0; var4 < var3.length; var4++) {
         var2[var4] = var3[var4].clone();
      }

      return var2;
   }
}
