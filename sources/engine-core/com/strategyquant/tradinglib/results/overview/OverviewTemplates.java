package com.strategyquant.tradinglib.results.overview;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.OverviewTemplate;

public class OverviewTemplates extends CustomClasses<OverviewTemplate> {
   private static OverviewTemplates instance;

   public static OverviewTemplates getInstance() {
      if (instance == null) {
         instance = new OverviewTemplates();
      }

      return instance;
   }

   private OverviewTemplates() {
      this.setDirName("Stats/Overview");
      this.setExpectedClassType(OverviewTemplate.class);
      this.loadAvailableClasses();
   }
}
