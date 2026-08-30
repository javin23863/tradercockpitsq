package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.ValuesMap;

public class SequentialOptimizationSettings {
   public int distributionUp;
   public int distributionDown;
   public int steps;
   public boolean applyToStrategy;
   public ValuesMap paramTypes;
   public boolean symmetry;
   public OptimizationParams manualParams;
   public int pctToPass;
   public int stableAreaCount;
   public int stabilityRangePct;

   public SequentialOptimizationSettings clone() {
      SequentialOptimizationSettings var1 = new SequentialOptimizationSettings();
      var1.distributionUp = this.distributionUp;
      var1.distributionDown = this.distributionDown;
      var1.steps = this.steps;
      var1.paramTypes = this.paramTypes != null ? this.paramTypes.clone() : null;
      var1.symmetry = this.symmetry;
      var1.manualParams = this.manualParams != null ? this.manualParams.clone() : null;
      var1.applyToStrategy = this.applyToStrategy;
      var1.pctToPass = this.pctToPass;
      var1.stableAreaCount = this.stableAreaCount;
      var1.stabilityRangePct = this.stabilityRangePct;
      return var1;
   }
}
