package com.strategyquant.tradinglib.correlation;

import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Databank;
import java.io.Serializable;

public class FitPortfolio implements Serializable {
   public boolean active;
   public Databank databank;
   public double corrMax;
   public CorrelationType corrType;
   public int corrPeriod;
   public boolean corrAllowNegative;
   public boolean corrAddEmptyPeriods;

   public FitPortfolio getClone() {
      FitPortfolio var1 = new FitPortfolio();
      var1.active = this.active;
      var1.databank = this.databank;
      var1.corrMax = this.corrMax;
      var1.corrType = this.corrType;
      var1.corrPeriod = this.corrPeriod;
      var1.corrAllowNegative = this.corrAllowNegative;
      var1.corrAddEmptyPeriods = this.corrAddEmptyPeriods;
      return var1;
   }
}
