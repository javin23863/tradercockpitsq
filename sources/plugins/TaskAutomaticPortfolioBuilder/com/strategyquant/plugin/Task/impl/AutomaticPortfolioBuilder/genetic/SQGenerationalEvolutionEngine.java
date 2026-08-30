package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.EvolutionUtils;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.termination.UserAbort;

public class SQGenerationalEvolutionEngine<T> implements EvolutionEngine<T> {
   private static FitnessEvaluationWorker concurrentWorker = null;
   private final Set<EvolutionObserver<? super T>> observers = new CopyOnWriteArraySet<>();
   private final Random rng;
   private final CandidateFactory<T> candidateFactory;
   private volatile boolean singleThreaded = false;
   private List<TerminationCondition> satisfiedTerminationConditions;
   private final EvolutionaryOperator<T> evolutionScheme;
   private final FitnessEvaluator<? super T> fitnessEvaluator;
   private final SelectionStrategy<? super T> selectionStrategy;
   private UserAbort userAbort;

   public SQGenerationalEvolutionEngine(
      CandidateFactory<T> var1, EvolutionaryOperator<T> var2, FitnessEvaluator<? super T> var3, SelectionStrategy<? super T> var4, Random var5, UserAbort var6
   ) {
      this.candidateFactory = var1;
      this.fitnessEvaluator = var3;
      this.rng = var5;
      this.evolutionScheme = var2;
      this.selectionStrategy = var4;
      this.userAbort = var6;
   }

   protected List<EvaluatedCandidate<T>> nextEvolutionStep(List<EvaluatedCandidate<T>> var1, int var2, Random var3) {
      ArrayList var4 = new ArrayList(var1.size());
      ArrayList var5 = new ArrayList(var2);
      Iterator var6 = var1.iterator();

      while (var5.size() < var2) {
         var5.add(((EvaluatedCandidate)var6.next()).getCandidate());
      }

      var4.addAll(this.selectionStrategy.select(var1, this.fitnessEvaluator.isNatural(), var1.size() - var2, var3));
      List var7 = this.evolutionScheme.apply(var4, var3);
      var7.addAll(var5);
      return this.evaluatePopulation(var7);
   }

   public T evolve(int var1, int var2, TerminationCondition... var3) {
      return this.evolve(var1, var2, Collections.emptySet(), var3);
   }

   public T evolve(int var1, int var2, Collection<T> var3, TerminationCondition... var4) {
      List var5 = this.evolvePopulation(var1, var2, var3, var4);
      return (T)(var5 == null ? null : ((EvaluatedCandidate)var5.get(0)).getCandidate());
   }

   public List<EvaluatedCandidate<T>> evolvePopulation(int var1, int var2, TerminationCondition... var3) {
      return this.evolvePopulation(var1, var2, Collections.emptySet(), var3);
   }

   public List<EvaluatedCandidate<T>> evolvePopulation(int var1, int var2, Collection<T> var3, TerminationCondition... var4) {
      if (var2 >= 0 && var2 < var1) {
         if (var4.length == 0) {
            throw new IllegalArgumentException("At least one TerminationCondition must be specified.");
         }

         this.satisfiedTerminationConditions = null;
         int var5 = 0;
         long var6 = System.currentTimeMillis();
         List var8 = this.candidateFactory.generateInitialPopulation(var1, var3, this.rng);
         List var9 = this.evaluatePopulation(var8);
         if (var9 == null) {
            return null;
         }

         EvolutionUtils.sortEvaluatedPopulation(var9, this.fitnessEvaluator.isNatural());
         PopulationData var10 = EvolutionUtils.getPopulationData(var9, this.fitnessEvaluator.isNatural(), var2, var5, var6);
         this.notifyPopulationChange(var10);
         List var11 = EvolutionUtils.shouldContinue(var10, var4);

         while (var11 == null) {
            var5++;
            var9 = this.nextEvolutionStep(var9, var2, this.rng);
            if (var9 == null) {
               return null;
            }

            EvolutionUtils.sortEvaluatedPopulation(var9, this.fitnessEvaluator.isNatural());
            var10 = EvolutionUtils.getPopulationData(var9, this.fitnessEvaluator.isNatural(), var2, var5, var6);
            this.notifyPopulationChange(var10);
            var11 = EvolutionUtils.shouldContinue(var10, var4);
         }

         this.satisfiedTerminationConditions = var11;
         return var9;
      } else {
         throw new IllegalArgumentException("Elite count must be non-negative and less than population size.");
      }
   }

   protected List<EvaluatedCandidate<T>> evaluatePopulation(List<T> var1) {
      ArrayList var2 = new ArrayList(var1.size());
      if (this.singleThreaded) {
         for (Object var4 : var1) {
            if (this.userAbort != null && this.userAbort.isAborted()) {
               return null;
            }

            var2.add(new EvaluatedCandidate(var4, this.fitnessEvaluator.getFitness(var4, var1)));
         }
      } else {
         try {
            List var9 = Collections.unmodifiableList(var1);
            ArrayList var10 = new ArrayList(var1.size());

            for (Object var6 : var1) {
               if (this.userAbort != null && this.userAbort.isAborted()) {
                  this.cancelResults(var10);
                  return null;
               }

               var10.add(getSharedWorker().submit(new FitnessEvalutationTask<>(this.fitnessEvaluator, var6, var9)));
            }

            for (Future var12 : var10) {
               if (this.userAbort != null && this.userAbort.isAborted()) {
                  this.cancelResults(var10);
                  return null;
               }

               var2.add((EvaluatedCandidate)var12.get());
            }
         } catch (ExecutionException var7) {
            throw new IllegalStateException("Fitness evaluation task execution failed.", var7);
         } catch (InterruptedException var8) {
            Thread.currentThread().interrupt();
         }
      }

      return var2;
   }

   private void cancelResults(List<Future<EvaluatedCandidate<T>>> var1) {
      for (Future var3 : var1) {
         var3.cancel(true);
      }
   }

   public List<TerminationCondition> getSatisfiedTerminationConditions() {
      if (this.satisfiedTerminationConditions == null) {
         throw new IllegalStateException("EvolutionEngine has not terminated.");
      } else {
         return Collections.unmodifiableList(this.satisfiedTerminationConditions);
      }
   }

   public void addEvolutionObserver(EvolutionObserver<? super T> var1) {
      this.observers.add(var1);
   }

   public void removeEvolutionObserver(EvolutionObserver<? super T> var1) {
      this.observers.remove(var1);
   }

   private void notifyPopulationChange(PopulationData<T> var1) {
      for (EvolutionObserver var3 : this.observers) {
         var3.populationUpdate(var1);
      }
   }

   public void setSingleThreaded(boolean var1) {
      this.singleThreaded = var1;
   }

   private static synchronized FitnessEvaluationWorker getSharedWorker() {
      if (concurrentWorker == null) {
         concurrentWorker = new FitnessEvaluationWorker();
      }

      return concurrentWorker;
   }
}
