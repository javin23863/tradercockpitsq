package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.Random;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

public class PortfolioGenomeFactory extends AbstractCandidateFactory<PortfolioGenome> {
   private GenomeSettings settings;

   public PortfolioGenomeFactory(GenomeSettings var1) {
      this.settings = var1;
   }

   public PortfolioGenome generateRandomCandidate(Random var1) {
      int var2 = this.settings.minStrategiesInPortfolio;
      if (this.settings.maxStrategiesInPortfolio > this.settings.minStrategiesInPortfolio) {
         var2 += var1.nextInt(this.settings.maxStrategiesInPortfolio - this.settings.minStrategiesInPortfolio + 1);
      }

      PortfolioGenome.StrategyGene[] var3 = new PortfolioGenome.StrategyGene[var2];

      for (int var4 = 0; var4 < 1000; var4++) {
         this.generatePortfolio(var3);
         if (PortfolioGenome.checkPortfolioIsCorrect(var3)) {
            return new PortfolioGenome(var3);
         }
      }

      throw new IllegalArgumentException("Cannot generate any portfolio under these settings!");
   }

   private void generatePortfolio(PortfolioGenome.StrategyGene[] var1) {
      for (int var2 = 0; var2 < var1.length; var2++) {
         int var3 = this.settings.rng.nextInt(this.settings.numberOfStrategies - var2);
         var1[var2] = new PortfolioGenome.StrategyGene(PortfolioGenome.findXthAvailableGeneNumber(var3, var1, var2, this.settings.numberOfStrategies));
      }
   }
}
