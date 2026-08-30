package com.strategyquant.tradinglib.montecarlo.retest;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.MonteCarloRetest;

public class MonteCarloRetestList extends CustomClasses<MonteCarloRetest> {
   private static MonteCarloRetestList instance;

   public static MonteCarloRetestList get() {
      if (instance == null) {
         instance = new MonteCarloRetestList();
      }

      return instance;
   }

   private MonteCarloRetestList() {
      this.setDirName("MonteCarlo/Retest");
      this.setExpectedClassType(MonteCarloRetest.class);
      this.loadAvailableClasses();
   }
}
