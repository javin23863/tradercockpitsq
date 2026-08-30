package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.genetic;

import java.util.Random;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.termination.UserAbort;

public class GenomeSettings {
   public int numberOfStrategies;
   public int minStrategiesInPortfolio = 0;
   public int maxStrategiesInPortfolio = 0;
   public Random rng;
   public int crossoverPoints;
   public Probability crossoverProbability;
   public int mutationPoints;
   public Probability mutationProbability;
   public Probability mutationResizeProbability;
   public boolean fitnessMaximize;
   public int populationSize;
   public int elitism;
   public boolean singleThreaded;
   public UserAbort userAbort;
   public int islands;
   public double IsPct;
   public boolean reverseSample;
}
