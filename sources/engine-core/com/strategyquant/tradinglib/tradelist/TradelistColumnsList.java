package com.strategyquant.tradinglib.tradelist;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.TradelistColumn;

public class TradelistColumnsList extends CustomClasses<TradelistColumn> {
   private static TradelistColumnsList instance;

   public static TradelistColumnsList get() {
      if (instance == null) {
         instance = new TradelistColumnsList();
      }

      return instance;
   }

   private TradelistColumnsList() {
      this.setDirName("Columns/Trades");
      this.setExpectedClassType(TradelistColumn.class);
      this.loadAvailableClasses();
   }
}
