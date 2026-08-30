package com.strategyquant.tradinglib.crosscheck;

import java.util.Comparator;

public class CrossCheckComparator implements Comparator<ICrossCheck> {
   public int compare(ICrossCheck var1, ICrossCheck var2) {
      int var3 = var1.getType() - var2.getType();
      return var3 == 0 ? var1.getPreferredPosition() - var2.getPreferredPosition() : var3;
   }
}
