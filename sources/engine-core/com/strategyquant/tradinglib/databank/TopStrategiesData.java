package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopStrategiesData {
   public static final Logger Log = LoggerFactory.getLogger(TopStrategiesData.class);
   public static int TOP_AVG_COUNT = 10;
   public static int TOP_STR_COUNT = 3;
   private double[][] topFitnesses = new double[5][TOP_AVG_COUNT];
   private double[] sumFitnesses = new double[5];
   private ResultsGroup[] topResults = new ResultsGroup[3];
   private double[] topResultsFitness = new double[3];
   private int fitnessesAdded;
   private int totalFitnessesAdded;

   public void reset() {
      for (int var1 = 0; var1 < 5; var1++) {
         for (int var2 = 0; var2 < this.topFitnesses.length; var2++) {
            this.topFitnesses[var1][var2] = 0.0;
            this.sumFitnesses[var1] = 0.0;
         }
      }

      for (int var3 = 0; var3 < this.topResults.length; var3++) {
         this.topResults[var3] = null;
         this.topResultsFitness[var3] = 0.0;
      }

      this.fitnessesAdded = 0;
      this.totalFitnessesAdded = 0;
   }

   public void updateWith(ResultsGroup var1) {
      this.updateFitnessWith(0, this.getFitness(var1, (byte)10), this.fitnessesAdded);
      this.updateFitnessWith(1, this.getFitness(var1, (byte)11), this.fitnessesAdded);
      this.updateFitnessWith(2, this.getFitness(var1, (byte)40), this.fitnessesAdded);
      this.updateFitnessWith(3, this.getFitness(var1, (byte)20), this.fitnessesAdded);
      boolean var2 = this.updateFitnessWith(4, this.getFitness(var1, (byte)127), this.fitnessesAdded);
      if (this.fitnessesAdded < this.topFitnesses.length) {
         this.fitnessesAdded++;
      }

      this.totalFitnessesAdded++;
      if (var2) {
         this.updateTopStrategiesWith(var1, this.getFitness(var1, (byte)127));
      }
   }

   private boolean updateFitnessWith(int var1, double var2, int var4) {
      this.sumFitnesses[var1] = this.sumFitnesses[var1] + var2;
      if (var4 < this.topFitnesses.length) {
         this.topFitnesses[var1][var4] = var2;
         return true;
      }

      int var5 = 0;
      double var6 = Double.MAX_VALUE;

      for (int var8 = 0; var8 < this.topFitnesses.length; var8++) {
         if (this.topFitnesses[var1][var8] < var6) {
            var6 = this.topFitnesses[var1][var8];
            var5 = var8;
         }
      }

      if (this.topFitnesses[var1][var5] < var2) {
         this.topFitnesses[var1][var5] = var2;
         return true;
      } else {
         return false;
      }
   }

   private void updateTopStrategiesWith(ResultsGroup var1, double var2) {
      boolean var4 = false;

      for (int var5 = 0; var5 < this.topResults.length; var5++) {
         if (this.topResults[var5] == null) {
            this.topResults[var5] = var1;
            this.topResultsFitness[var5] = var2;
            var4 = true;
            break;
         }
      }

      if (!var4) {
         double var9 = this.topResultsFitness[this.topResults.length - 1];
         if (var2 > var9) {
            this.topResults[this.topResults.length - 1] = var1;
            this.topResultsFitness[this.topResults.length - 1] = var2;
         }
      }

      for (int var10 = this.topResults.length - 1; var10 > 0; var10--) {
         if (this.topResults[var10] != null && this.topResultsFitness[var10] > this.topResultsFitness[var10 - 1]) {
            ResultsGroup var6 = this.topResults[var10];
            this.topResults[var10] = this.topResults[var10 - 1];
            this.topResults[var10 - 1] = var6;
            double var7 = this.topResultsFitness[var10];
            this.topResultsFitness[var10] = this.topResultsFitness[var10 - 1];
            this.topResultsFitness[var10 - 1] = var7;
         }
      }
   }

   public ResultsGroup[] getTopResults() {
      return this.topResults;
   }

   private double getFitness(ResultsGroup var1, byte var2) {
      try {
         double var3 = var1.portfolio().getFitness(var2);
         return var3 > 0.0 ? var3 : 0.0;
      } catch (Exception var5) {
         Log.error("Exc.", var5);
         return 0.0;
      }
   }

   public void updateDatabankFitnesses(FitnessCollectionData var1) {
      this.getFitnesses(0, this.topFitnesses[0], var1);
      this.getFitnesses(1, this.topFitnesses[1], var1);
      this.getFitnesses(2, this.topFitnesses[2], var1);
      this.getFitnesses(3, this.topFitnesses[3], var1);
      this.getFitnesses(4, this.topFitnesses[4], var1);
   }

   private void getFitnesses(int var1, double[] var2, FitnessCollectionData var3) {
      if (this.fitnessesAdded == 0) {
         switch (var1) {
            case 0:
               var3.bestFitnessIS = 0.0;
               var3.topAvgFitnessIS = 0.0;
               var3.avgFitnessIS = 0.0;
               break;
            case 1:
               var3.bestFitnessIST = 0.0;
               var3.topAvgFitnessIST = 0.0;
               var3.avgFitnessIST = 0.0;
               break;
            case 2:
               var3.bestFitnessISV = 0.0;
               var3.topAvgFitnessISV = 0.0;
               var3.avgFitnessISV = 0.0;
               break;
            case 3:
               var3.bestFitnessOOS = 0.0;
               var3.topAvgFitnessOOS = 0.0;
               var3.avgFitnessOOS = 0.0;
               break;
            case 4:
               var3.bestFitnessFS = 0.0;
               var3.topAvgFitnessFS = 0.0;
               var3.avgFitnessFS = 0.0;
         }
      } else {
         double var4 = 0.0;
         double var6 = -1.0;

         for (int var8 = 0; var8 < this.fitnessesAdded; var8++) {
            var4 += var2[var8];
            if (var2[var8] > var6) {
               var6 = var2[var8];
            }
         }

         switch (var1) {
            case 0:
               var3.bestFitnessIS = var6;
               var3.topAvgFitnessIS = var4 / this.fitnessesAdded;
               var3.avgFitnessIS = this.sumFitnesses[0] / this.totalFitnessesAdded;
               break;
            case 1:
               var3.bestFitnessIST = var6;
               var3.topAvgFitnessIST = var4 / this.fitnessesAdded;
               var3.avgFitnessIST = this.sumFitnesses[1] / this.totalFitnessesAdded;
               break;
            case 2:
               var3.bestFitnessISV = var6;
               var3.topAvgFitnessISV = var4 / this.fitnessesAdded;
               var3.avgFitnessISV = this.sumFitnesses[2] / this.totalFitnessesAdded;
               break;
            case 3:
               var3.bestFitnessOOS = var6;
               var3.topAvgFitnessOOS = var4 / this.fitnessesAdded;
               var3.avgFitnessOOS = this.sumFitnesses[3] / this.totalFitnessesAdded;
               break;
            case 4:
               var3.bestFitnessFS = var6;
               var3.topAvgFitnessFS = var4 / this.fitnessesAdded;
               var3.avgFitnessFS = this.sumFitnesses[4] / this.totalFitnessesAdded;
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder("\nTOP STRS\n");

      for (int var2 = 0; var2 < this.topResults.length; var2++) {
         if (this.topResults[var2] != null) {
            var1.append("#");
            var1.append(var2);
            var1.append(this.topResults[var2].getName());
            var1.append(" - ");
            var1.append(SQUtils.round2(this.topResultsFitness[var2]));
            var1.append("\n");
         }
      }

      var1.append("-----------------------------------");
      return var1.toString();
   }
}
