package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

public class PortfolioGenomeMutation implements EvolutionaryOperator<PortfolioGenome> {
   private GenomeSettings settings;

   public PortfolioGenomeMutation(GenomeSettings var1) {
      this.settings = var1;
   }

   public List<PortfolioGenome> apply(List<PortfolioGenome> var1, Random var2) {
      ArrayList var3 = new ArrayList(var1.size());

      for (PortfolioGenome var5 : var1) {
         var3.add(this.mutate(var5, var2));
      }

      return var3;
   }

   private PortfolioGenome mutate(PortfolioGenome var1, Random var2) {
      PortfolioGenome.StrategyGene[] var3 = var1.cloneGenes();
      boolean var4 = false;

      for (int var5 = 0; var5 < this.settings.mutationPoints; var5++) {
         if (this.settings.mutationProbability.nextEvent(var2)) {
            int var6 = var2.nextInt(var3.length);
            var3[var6] = this.mutateGene(var6, var3);
            var4 = true;
         }
      }

      if (this.settings.maxStrategiesInPortfolio != this.settings.minStrategiesInPortfolio && this.settings.mutationResizeProbability.nextEvent(var2)) {
         if (var3.length < this.settings.maxStrategiesInPortfolio) {
            var3 = this.addGene(var3, var2);
         } else if (var3.length > this.settings.minStrategiesInPortfolio) {
            var3 = this.removeGene(var3, var2.nextInt(var3.length));
         }
      }

      PortfolioGenome var7 = new PortfolioGenome(var3);
      if (var4) {
      }

      return var7;
   }

   private PortfolioGenome.StrategyGene[] addGene(PortfolioGenome.StrategyGene[] var1, Random var2) {
      PortfolioGenome.StrategyGene[] var3 = new PortfolioGenome.StrategyGene[var1.length + 1];

      for (int var4 = 0; var4 < var1.length; var4++) {
         var3[var4] = var1[var4];
      }

      int var5 = this.settings.rng.nextInt(this.settings.numberOfStrategies - var1.length);
      var3[var1.length] = new PortfolioGenome.StrategyGene(
         PortfolioGenome.findXthAvailableGeneNumber(var5, var1, var1.length, this.settings.numberOfStrategies)
      );
      return var3;
   }

   private PortfolioGenome.StrategyGene[] removeGene(PortfolioGenome.StrategyGene[] var1, int var2) {
      PortfolioGenome.StrategyGene[] var3 = new PortfolioGenome.StrategyGene[var1.length - 1];
      int var4 = 0;
      int var5 = 0;

      while (var4 < var1.length) {
         if (var4 != var2) {
            var3[var5++] = var1[var4];
         }

         var4++;
      }

      return var3;
   }

   private PortfolioGenome.StrategyGene mutateGene(int var1, PortfolioGenome.StrategyGene[] var2) {
      int var3 = this.settings.rng.nextInt(this.settings.numberOfStrategies - var2.length);
      return new PortfolioGenome.StrategyGene(PortfolioGenome.findXthAvailableGeneNumber(var3, var2, var2.length, this.settings.numberOfStrategies));
   }
}
