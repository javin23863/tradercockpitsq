package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.SQUtils;

public class FitnessCollectionData {
   private int generation;
   public double bestFitnessIS;
   public double bestFitnessIST;
   public double bestFitnessISV;
   public double bestFitnessOOS;
   public double bestFitnessFS;
   public double topAvgFitnessIS;
   public double topAvgFitnessIST;
   public double topAvgFitnessISV;
   public double topAvgFitnessOOS;
   public double topAvgFitnessFS;
   public double avgFitnessIS;
   public double avgFitnessIST;
   public double avgFitnessISV;
   public double avgFitnessOOS;
   public double avgFitnessFS;

   public FitnessCollectionData(int var1) {
      this.generation = var1;
   }

   public void reset() {
      this.bestFitnessIS = 0.0;
      this.bestFitnessISV = 0.0;
      this.bestFitnessIST = 0.0;
      this.bestFitnessOOS = 0.0;
      this.bestFitnessFS = 0.0;
      this.topAvgFitnessIS = 0.0;
      this.topAvgFitnessISV = 0.0;
      this.topAvgFitnessIST = 0.0;
      this.topAvgFitnessOOS = 0.0;
      this.topAvgFitnessFS = 0.0;
      this.avgFitnessIS = 0.0;
      this.avgFitnessISV = 0.0;
      this.avgFitnessIST = 0.0;
      this.avgFitnessOOS = 0.0;
      this.avgFitnessFS = 0.0;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append("\nBST IS:");
      var1.append(SQUtils.round2(this.bestFitnessIS));
      var1.append(", ISV:");
      var1.append(SQUtils.round2(this.bestFitnessISV));
      var1.append(", IST:");
      var1.append(SQUtils.round2(this.bestFitnessIST));
      var1.append(", OOS:");
      var1.append(SQUtils.round2(this.bestFitnessOOS));
      var1.append(", FS:");
      var1.append(SQUtils.round2(this.bestFitnessFS));
      var1.append("\n");
      var1.append("TOP IS:");
      var1.append(SQUtils.round2(this.topAvgFitnessIS));
      var1.append(", ISV:");
      var1.append(SQUtils.round2(this.topAvgFitnessISV));
      var1.append(", IST:");
      var1.append(SQUtils.round2(this.topAvgFitnessIST));
      var1.append(", OOS:");
      var1.append(SQUtils.round2(this.topAvgFitnessOOS));
      var1.append(", FS:");
      var1.append(SQUtils.round2(this.topAvgFitnessFS));
      var1.append("\n");
      var1.append("AVG IS:");
      var1.append(SQUtils.round2(this.avgFitnessIS));
      var1.append(", ISV:");
      var1.append(SQUtils.round2(this.avgFitnessISV));
      var1.append(", IST:");
      var1.append(SQUtils.round2(this.avgFitnessIST));
      var1.append(", OOS:");
      var1.append(SQUtils.round2(this.avgFitnessOOS));
      var1.append(", FS:");
      var1.append(SQUtils.round2(this.avgFitnessFS));
      var1.append("\n");
      var1.append("-----------------------------------");
      return var1.toString();
   }
}
