package com.strategyquant.tradinglib.project;

import com.strategyquant.tradinglib.Databank;
import java.util.Comparator;

public class DatabankPositionComparator implements Comparator<Databank> {
   public int compare(Databank var1, Databank var2) {
      if (var1.isDefault() && var2.isDefault()) {
         if (var1.getName().equals("Results")) {
            return -1;
         } else {
            return var2.getName().equals("Results") ? 1 : 0;
         }
      } else if (var1.isDefault() && !var2.isDefault()) {
         return -1;
      } else {
         return !var1.isDefault() && var2.isDefault() ? 1 : var1.getPosition() - var2.getPosition();
      }
   }
}
