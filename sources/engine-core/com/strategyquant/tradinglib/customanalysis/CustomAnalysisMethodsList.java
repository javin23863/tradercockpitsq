package com.strategyquant.tradinglib.customanalysis;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.CustomAnalysisMethod;

public class CustomAnalysisMethodsList extends CustomClasses<CustomAnalysisMethod> {
   private static CustomAnalysisMethodsList instance;

   public static CustomAnalysisMethodsList get() {
      if (instance == null) {
         instance = new CustomAnalysisMethodsList();
      }

      return instance;
   }

   private CustomAnalysisMethodsList() {
      this.setDirName("CustomAnalysis");
      this.setExpectedClassType(CustomAnalysisMethod.class);
      this.loadAvailableClasses();
   }
}
