package com.strategyquant.tradinglib.engine.stockpicker.signals.entry;

import java.io.Serializable;
import java.util.Comparator;

public class PickerEntrySignalComparatorByRevScore implements Comparator<PickerEntrySignal>, Serializable {
   public int compare(PickerEntrySignal var1, PickerEntrySignal var2) {
      double var3 = var1.posScore;
      double var5 = var2.posScore;
      if (var3 == var5) {
         return var2.symbol.compareTo(var1.symbol);
      } else {
         return var3 < var5 ? -1 : 1;
      }
   }
}
