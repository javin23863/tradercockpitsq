package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.wfo.WFVariant;
import java.util.Comparator;

public class VariantComparatorByName implements Comparator<WFVariant> {
   public int compare(WFVariant var1, WFVariant var2) {
      return var1.params.compareTo(var2.params);
   }
}
