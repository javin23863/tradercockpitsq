package com.strategyquant.tradinglib.robustnesstests;

import java.util.ArrayList;

public class MCSimulations extends ArrayList<RobustnessResults> {
   public MCSimulations getClone() throws Exception {
      MCSimulations var1 = new MCSimulations();

      for (int var2 = 0; var2 < this.size(); var2++) {
         var1.add(this.get(var2).getClone());
      }

      return var1;
   }
}
