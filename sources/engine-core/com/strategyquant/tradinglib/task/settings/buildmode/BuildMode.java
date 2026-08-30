package com.strategyquant.tradinglib.task.settings.buildmode;

import com.strategyquant.tradinglib.conditions.Condition;
import java.io.Serializable;
import java.util.ArrayList;

public class BuildMode implements Serializable {
   public String generationType;
   public boolean evoRestartOnStagnation;
   public int populationSize;
   public int maxGenerations;
   public int initGenerationType;
   public int evoFitnessRestartType;
   public int evoStagnationRestartGenerations;
   public int mutationProbability;
   public int crossoverProbability;
   public int decimationCoef;
   public int islands;
   public int migrationModulo;
   public int migrationRate;
   public ArrayList<Condition> conditions;
   public boolean evoRestartOnFinish;
   public boolean showLastGeneration;
   public boolean freshBloodReplaceSimilar;
   public boolean freshBloodReplaceWeakest;
   public int freshBloodWeakestPct;
   public int freshBloodWeakestGenerations;
   public boolean evoDivideInSamplePeriod;
   public double evoTrainingValidationRatio;
}
