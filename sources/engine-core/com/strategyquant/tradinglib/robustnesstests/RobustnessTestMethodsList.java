package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.lib.snippets.CustomClasses;

public class RobustnessTestMethodsList extends CustomClasses<RobustnessTestMethod> {
   private static RobustnessTestMethodsList instance;

   public static RobustnessTestMethodsList get() {
      if (instance == null) {
         instance = new RobustnessTestMethodsList();
      }

      return instance;
   }

   private RobustnessTestMethodsList() {
      this.setDirName("RobustnessTests");
      this.setExpectedClassType(RobustnessTestMethod.class);
      this.loadAvailableClasses();
   }
}
