package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PMComputationListener;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.PopulationData;

public class EvolutionLogger implements EvolutionObserver<PortfolioGenome> {
   private PMComputationListener listener;

   public EvolutionLogger(PMComputationListener var1) {
      this.listener = var1;
   }

   public void populationUpdate(PopulationData<? extends PortfolioGenome> var1) {
      this.listener.printLog("-------- Generation " + var1.getGenerationNumber());
      ((PortfolioGenome)var1.getBestCandidate()).setGeneration(var1.getGenerationNumber());
      ((PortfolioGenome)var1.getBestCandidate()).setFitness(var1.getBestCandidateFitness());
   }
}
