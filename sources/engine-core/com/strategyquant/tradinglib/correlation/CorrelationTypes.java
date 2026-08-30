package com.strategyquant.tradinglib.correlation;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.CorrelationType;

public class CorrelationTypes extends CustomClasses<CorrelationType> {
   private static CorrelationTypes instance;

   public static CorrelationTypes getInstance() {
      if (instance == null) {
         instance = new CorrelationTypes();
      }

      return instance;
   }

   private CorrelationTypes() {
      this.setDirName("CorrelationOf");
      this.setExpectedClassType(CorrelationType.class);
      this.loadAvailableClasses();
   }
}
