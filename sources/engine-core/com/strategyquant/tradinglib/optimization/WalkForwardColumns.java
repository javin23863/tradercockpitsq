package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.WalkForwardColumn;

public class WalkForwardColumns extends CustomClasses<WalkForwardColumn> {
   private static WalkForwardColumns instance;

   public static WalkForwardColumns get() {
      if (instance == null) {
         instance = new WalkForwardColumns();
      }

      return instance;
   }

   private WalkForwardColumns() {
      this.setDirName("Columns/WalkForward");
      this.setExpectedClassType(WalkForwardColumn.class);
      this.loadAvailableClasses();
   }
}
