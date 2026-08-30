package com.strategyquant.tradinglib.portfolioMaster;

import com.strategyquant.tradinglib.IFitnessValue;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.debug.Debugger;

public abstract class PortfolioMasterFitness extends Debugger implements IFitnessValue {
   public static String SampleKeyIS = "IS";
   public static String SampleKeyFull = "Full";
   protected String name = null;
   protected byte valueType = 1;
   private double target;
   private double avgMin;
   private double avgMax;

   public PortfolioMasterFitness(String var1, byte var2, double var3, double var5, double var7) {
      this.name = var1;
      this.valueType = var2;
      this.target = var3;
      this.avgMin = var5;
      this.avgMax = var7;
   }

   public String getName() {
      return this.name;
   }

   public int getValueType() {
      return this.valueType;
   }

   public abstract double compute(ResultsGroup var1, byte var2) throws Exception;

   public double transformToFitnessRange(double var1) throws Exception {
      double var3 = this.transformToFitnessRange(var1, this.target, this.avgMin, this.avgMax, this.valueType);
      if (var3 < 0.0) {
         var3 = 0.0;
      }

      if (var3 > 1.0) {
         var3 = 1.0;
      }

      return var3;
   }

   public abstract String print(double var1) throws Exception;
}
