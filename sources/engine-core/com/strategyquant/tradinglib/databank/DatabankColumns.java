package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.DatabankColumn;

public class DatabankColumns extends CustomClasses<DatabankColumn> {
   private static DatabankColumns instance;

   public static DatabankColumns get() {
      if (instance == null) {
         instance = new DatabankColumns();
      }

      return instance;
   }

   private DatabankColumns() {
      this.setDirName("Columns/Databanks");
      this.setExpectedClassType(DatabankColumn.class);
      this.loadAvailableClasses();
   }
}
