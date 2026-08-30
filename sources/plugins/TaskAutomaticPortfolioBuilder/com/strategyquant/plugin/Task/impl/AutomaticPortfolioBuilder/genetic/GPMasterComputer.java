package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce.PMComputationListener;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import java.util.ArrayList;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.UserAbort;

public class GPMasterComputer {
   public static UserAbort userAbort = new UserAbort();

   public static void run(PMComputationListener var0, PortfolioMasterFitness var1, int var2, int var3, int var4, PortfolioMasterSettings var5) {
      int var6 = var5.populationSize;
      int var7 = var5.generations;
      boolean var8 = var5.runContinually;
      boolean var9 = var5.restartAfterStagnation;
      int var10 = var5.generationLimit;

      while (true) {
         GenomeSettings var11 = new GenomeSettings();
         var11.IsPct = var5.IsPct;
         var11.reverseSample = var5.reverseSample;
         var11.numberOfStrategies = var2;
         var11.minStrategiesInPortfolio = var3;
         var11.maxStrategiesInPortfolio = var4;
         var11.populationSize = var6;
         var11.elitism = 1;
         var11.fitnessMaximize = var5.fitnessFunction.getValueType() == 1;
         var11.singleThreaded = false;
         var11.userAbort = userAbort;
         var11.crossoverPoints = 1;
         var11.crossoverProbability = new Probability(0.8);
         var11.mutationPoints = 1;
         var11.mutationProbability = new Probability(0.5);
         var11.mutationResizeProbability = new Probability(0.1);
         var11.rng = new MersenneTwisterRNG();
         var11.userAbort.reset();
         ArrayList var12 = new ArrayList(2);
         var12.add(new PortfolioGenomeMutation(var11));
         EvolutionPipeline var13 = new EvolutionPipeline(var12);
         SQGenerationalEvolutionEngine var14 = new SQGenerationalEvolutionEngine(
            new PortfolioGenomeFactory(var11), var13, new PortfolioGenomeEvaluator(var11, var0, var5), new RouletteWheelSelection(), var11.rng, var11.userAbort
         );
         var14.addEvolutionObserver(new EvolutionLogger(var0));
         if (var11.singleThreaded) {
            var14.setSingleThreaded(true);
         }

         if (var9) {
            PortfolioGenome var15 = (PortfolioGenome)var14.evolve(
               var11.populationSize, var11.elitism, new GenerationCount(var7), new Stagnation(var10, true, true), var11.userAbort
            );
         } else {
            PortfolioGenome var16 = (PortfolioGenome)var14.evolve(var11.populationSize, var11.elitism, new GenerationCount(var7), var11.userAbort);
         }

         if (!var8 || var0.isStopped()) {
            return;
         }

         var0.printLog("New genetic run =================================================");
         var0.resetStats();
      }
   }
}
