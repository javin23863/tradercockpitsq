package com.strategyquant.tradinglib.portfolioMaster;

import com.strategyquant.lib.snippets.CustomClasses;

public class PortfolioMasterFitnessList extends CustomClasses<PortfolioMasterFitness> {
   private static PortfolioMasterFitnessList instance;

   public static PortfolioMasterFitnessList getInstance() {
      if (instance == null) {
         instance = new PortfolioMasterFitnessList();
      }

      return instance;
   }

   private PortfolioMasterFitnessList() {
      this.setDirName("PortfolioMasterFitness");
      this.setExpectedClassType(PortfolioMasterFitness.class);
      this.loadAvailableClasses();
   }
}
