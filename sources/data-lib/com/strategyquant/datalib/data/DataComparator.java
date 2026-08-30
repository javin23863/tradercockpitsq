package com.strategyquant.datalib.data;

import com.strategyquant.datalib.DataInfo;
import java.util.Comparator;

public class DataComparator implements Comparator<DataInfo> {
   public int compare(DataInfo var1, DataInfo var2) {
      return var1.symbol.compareToIgnoreCase(var2.symbol);
   }
}
