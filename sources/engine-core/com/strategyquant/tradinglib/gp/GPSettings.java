package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.io.Serializable;

public class GPSettings<T extends IGPNode> implements Serializable {
   private static final long serialVersionUID = 1L;
   public int populationSize;
   public int elitismSize;
   public int maxGenerations;
   public int numberOfIslands;
   public double migrationRate;
   public int migrationGenerationsModulo;
   public IRandomGenerator rng;
   public int decimationCoefficient;
   public boolean naturalFitness = true;
   public AbstractFactory<T> factory;
   public EvolutionPipeline<T> evolutionPipeline;
   public IFitnessEvaluator<? super T> evaluator;
   public ISelectionStrategy<? super T> selectionStrategy;
   public boolean printPopulationToLog = false;
   public boolean sendEveryCandidateEvent = true;
   public boolean sendLastPopulation = false;
   public boolean singleThreaded = false;
   public boolean restartOnFinish;
   public boolean restartOnStagnation;
   public int stagnationGenerationsToRestart;
   public int stagnationFitnessRestartType;
   public int evolutionUniquenessRetries = 0;
   public boolean freshBloodReplaceSimilar = false;
   public boolean freshBloodReplaceWeakest = false;
   public int replaceWeakestPct = 10;
   public int replaceWeakestGenerations = 5;
   public double trainingValidationRatio = 0.5;

   public GPSettings<T> clone(IRandomGenerator var1) {
      GPSettings var2 = new GPSettings();
      var2.populationSize = this.populationSize;
      var2.elitismSize = this.elitismSize;
      var2.maxGenerations = this.maxGenerations;
      var2.numberOfIslands = this.numberOfIslands;
      var2.migrationRate = this.migrationRate;
      var2.migrationGenerationsModulo = this.migrationGenerationsModulo;
      var2.rng = var1;
      var2.decimationCoefficient = this.decimationCoefficient;
      var2.naturalFitness = this.naturalFitness;
      var2.factory = this.factory.clone();
      var2.evolutionPipeline = this.evolutionPipeline;
      var2.evaluator = this.evaluator;
      var2.selectionStrategy = this.selectionStrategy;
      var2.printPopulationToLog = this.printPopulationToLog;
      var2.sendEveryCandidateEvent = this.sendEveryCandidateEvent;
      var2.sendLastPopulation = this.sendLastPopulation;
      var2.singleThreaded = this.singleThreaded;
      var2.restartOnFinish = this.restartOnFinish;
      var2.restartOnStagnation = this.restartOnStagnation;
      var2.stagnationGenerationsToRestart = this.stagnationGenerationsToRestart;
      var2.stagnationFitnessRestartType = this.stagnationFitnessRestartType;
      var2.trainingValidationRatio = this.trainingValidationRatio;
      var2.evolutionUniquenessRetries = this.evolutionUniquenessRetries;
      var2.freshBloodReplaceSimilar = this.freshBloodReplaceSimilar;
      var2.freshBloodReplaceWeakest = this.freshBloodReplaceWeakest;
      var2.replaceWeakestPct = this.replaceWeakestPct;
      var2.replaceWeakestGenerations = this.replaceWeakestGenerations;
      return var2;
   }
}
