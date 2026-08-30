package com.strategyquant.tradinglib.event;

import java.util.ArrayList;

public class TradingEventsArray {
   private ArrayList<TradingEventObj> tradingEventsArray = new ArrayList<>();

   public TradingEventObj getAvailableEventObject() {
      if (this.tradingEventsArray.size() > 0) {
         for (int var1 = 0; var1 < this.tradingEventsArray.size(); var1++) {
            TradingEventObj var2 = this.tradingEventsArray.get(0);
            if (var2.checkAndSetInUse()) {
               return var2;
            }
         }
      }

      TradingEventObj var3 = new TradingEventObj();
      var3.checkAndSetInUse();
      this.tradingEventsArray.add(var3);
      return var3;
   }
}
