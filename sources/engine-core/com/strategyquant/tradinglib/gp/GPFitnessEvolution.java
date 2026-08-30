package com.strategyquant.tradinglib.gp;

import com.strategyquant.tradinglib.databank.TopStrategiesData;
import java.util.ArrayList;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPFitnessEvolution<T extends IGPNode> {
   private static final Logger Log = LoggerFactory.getLogger("GPFitnessEvolution");
   private GPFitnessEvolutionData data;
   private GPSettings<T> gpSettings;
   GPFitnessComparator<T> gpComparatorIS;
   GPFitnessComparator<T> gpComparatorIST;
   GPFitnessComparator<T> gpComparatorISV;
   GPFitnessComparator<T> gpComparatorOOS;

   public GPFitnessEvolution(int var1, GPSettings<T> var2) {
      this.gpSettings = var2;
      this.data = new GPFitnessEvolutionData(var1, var2.maxGenerations);
      this.gpComparatorIS = new GPFitnessComparator<>(var2.naturalFitness, (byte)10);
      this.gpComparatorIST = new GPFitnessComparator<>(var2.naturalFitness, (byte)11);
      this.gpComparatorISV = new GPFitnessComparator<>(var2.naturalFitness, (byte)40);
      this.gpComparatorOOS = new GPFitnessComparator<>(var2.naturalFitness, (byte)20);
   }

   public void reset() {
      this.data.reset();
   }

   public void computeFitnessData(ArrayList<T> var1, int var2) {
      this.data.generation = var2;
      if (this.data.generationData[var2] == null) {
         this.data.generationData[var2] = new FitnessCollectionData(var2 + 1);
      }

      this.comuteFitnessDataFor((byte)10, var1, var2);
      this.comuteFitnessDataFor((byte)11, var1, var2);
      this.comuteFitnessDataFor((byte)40, var1, var2);
      this.comuteFitnessDataFor((byte)20, var1, var2);
   }

   private void comuteFitnessDataFor(byte var1, ArrayList<T> var2, int var3) {
      double var4 = 0.0;
      double var6 = 0.0;
      double var8 = 0.0;
      switch (var1) {
         case 10:
            Collections.sort(var2, this.gpComparatorIS);
            break;
         case 11:
            Collections.sort(var2, this.gpComparatorIST);
            break;
         case 20:
            Collections.sort(var2, this.gpComparatorOOS);
            break;
         case 40:
            Collections.sort(var2, this.gpComparatorISV);
      }

      for (int var10 = 0; var10 < var2.size(); var10++) {
         IGPNode var11 = (IGPNode)var2.get(var10);
         var4 = var11.getFitness(var1);
         var6 += var4;
         if (var10 < TopStrategiesData.TOP_AVG_COUNT) {
            var8 += var4;
         }
      }

      IGPNode var13 = (IGPNode)var2.get(0);
      int var14 = Math.min(TopStrategiesData.TOP_AVG_COUNT, var2.size());
      this.data.populationSize = var2.size();
      switch (var1) {
         case 10:
            this.data.generationData[var3].bestFitnessIS = var13.getFitness((byte)10);
            if (var14 > 0) {
               this.data.generationData[var3].topAvgFitnessIS = var8 / var14;
            }

            if (this.data.populationSize > 0) {
               this.data.generationData[var3].avgFitnessIS = var6 / this.data.populationSize;
            }
            break;
         case 11:
            this.data.generationData[var3].bestFitnessIST = var13.getFitness((byte)11);
            if (var14 > 0) {
               this.data.generationData[var3].topAvgFitnessIST = var8 / var14;
            }

            if (this.data.populationSize > 0) {
               this.data.generationData[var3].avgFitnessIST = var6 / this.data.populationSize;
            }
            break;
         case 20:
            this.data.generationData[var3].bestFitnessOOS = var13.getFitness((byte)20);
            if (var14 > 0) {
               this.data.generationData[var3].topAvgFitnessOOS = var8 / var14;
            }

            if (this.data.populationSize > 0) {
               this.data.generationData[var3].avgFitnessOOS = var6 / this.data.populationSize;
            }
            break;
         case 40:
            this.data.generationData[var3].bestFitnessISV = var13.getFitness((byte)40);
            if (var14 > 0) {
               this.data.generationData[var3].topAvgFitnessISV = var8 / var14;
            }

            if (this.data.populationSize > 0) {
               this.data.generationData[var3].avgFitnessISV = var6 / this.data.populationSize;
            }
      }
   }

   public boolean isStagnating() {
      if (!this.gpSettings.restartOnStagnation) {
         return false;
      }

      if (this.gpSettings.stagnationGenerationsToRestart > 0) {
         double var1 = 0.0;
         double var3 = 0.0;
         int var5 = 0;
         int var6 = this.data.generation - this.gpSettings.stagnationGenerationsToRestart;
         if (var6 < 0) {
            boolean var10 = false;
         }

         for (int var7 = 0; var7 <= this.data.generation; var7++) {
            FitnessCollectionData var8 = this.data.generationData[var7];
            if (var8 != null) {
               var1 = this.getFitnessBySampleType(var8, (byte)this.gpSettings.stagnationFitnessRestartType);
               if (var1 == 0.0) {
                  return false;
               }

               if (var7 == 0) {
                  var3 = var1;
               } else if (var1 <= var3) {
                  var5++;
               } else {
                  var3 = var1;
                  var5 = 0;
               }
            }
         }

         if (var5 >= this.gpSettings.stagnationGenerationsToRestart) {
            return true;
         }
      }

      return false;
   }

   private double getFitnessBySampleType(FitnessCollectionData var1, byte var2) {
      switch (var2) {
         case 10:
            return var1.topAvgFitnessIS;
         case 11:
            return var1.topAvgFitnessIST;
         case 40:
            return var1.topAvgFitnessISV;
         default:
            return var1.topAvgFitnessIS;
      }
   }

   public GPFitnessEvolutionData getData() {
      return this.data;
   }
}
