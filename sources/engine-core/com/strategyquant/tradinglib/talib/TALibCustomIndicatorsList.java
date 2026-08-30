package com.strategyquant.tradinglib.talib;

import com.strategyquant.lib.snippets.CustomClasses;

public class TALibCustomIndicatorsList extends CustomClasses<TALibCustomIndicator> {
   private static TALibCustomIndicatorsList instance;

   public static TALibCustomIndicatorsList getInstance() {
      if (instance == null) {
         instance = new TALibCustomIndicatorsList();
      }

      return instance;
   }

   private TALibCustomIndicatorsList() {
      this.setDirName("TALibIndicators");
      this.setExpectedClassType(TALibCustomIndicator.class);
      this.loadAvailableClasses();
   }
}
