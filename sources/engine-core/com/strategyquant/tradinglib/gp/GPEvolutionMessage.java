package com.strategyquant.tradinglib.gp;

import java.io.Serializable;

public class GPEvolutionMessage implements Serializable {
   public static final int InitialPopulationStarted = 10;
   public static final int InitialPopulationGenerated = 20;
   public static final int NewGeneration = 30;
   public static final int Finished = 40;
   public static final int InitialPopulationEvaluating = 50;
   public static final int RestartedAfterFinish = 60;
   public static final int RestartedAfterStagnation = 70;
   public static final int AdditionalPopulationStarted = 80;
   public static final int AdditionalPopulationEvaluating = 90;
   public static final int AdditionalPopulationGenerated = 100;
   public int messageType;
   public int currentGeneration;
   public int islandIndex;
   public String message;
   public int populationSize;
   public GPFitnessEvolutionData fitnessEvolutionData;

   public GPEvolutionMessage(int var1, int var2, int var3, int var4, String var5) {
      this.messageType = var1;
      this.currentGeneration = var2;
      this.islandIndex = var3;
      this.populationSize = var4;
      this.message = var5;
   }
}
