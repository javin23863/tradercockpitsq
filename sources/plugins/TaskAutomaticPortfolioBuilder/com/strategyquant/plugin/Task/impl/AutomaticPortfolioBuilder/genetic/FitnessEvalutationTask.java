package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.List;
import java.util.concurrent.Callable;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

class FitnessEvalutationTask<T> implements Callable<EvaluatedCandidate<T>> {
   private final FitnessEvaluator<? super T> fitnessEvaluator;
   private final T candidate;
   private final List<T> population;

   FitnessEvalutationTask(FitnessEvaluator<? super T> var1, T var2, List<T> var3) {
      this.fitnessEvaluator = var1;
      this.candidate = (T)var2;
      this.population = var3;
   }

   public EvaluatedCandidate<T> call() {
      return new EvaluatedCandidate(this.candidate, this.fitnessEvaluator.getFitness(this.candidate, this.population));
   }
}
