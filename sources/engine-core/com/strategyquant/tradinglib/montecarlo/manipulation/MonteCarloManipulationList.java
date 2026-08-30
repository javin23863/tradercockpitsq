package com.strategyquant.tradinglib.montecarlo.manipulation;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.MonteCarloManipulation;

public class MonteCarloManipulationList extends CustomClasses<MonteCarloManipulation> {
   private static MonteCarloManipulationList instance;

   public static MonteCarloManipulationList get() {
      if (instance == null) {
         instance = new MonteCarloManipulationList();
      }

      return instance;
   }

   private MonteCarloManipulationList() {
      this.setDirName("MonteCarlo/Manipulation");
      this.setExpectedClassType(MonteCarloManipulation.class);
      this.loadAvailableClasses();
   }
}
