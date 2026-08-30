package com.strategyquant.pluginlib;

import java.util.Comparator;
import net.xeoh.plugins.base.Plugin;

public interface ISQPlugin extends Plugin {
   Comparator<ISQPlugin> PositionComparator = new Comparator<ISQPlugin>() {
      public int compare(ISQPlugin var1, ISQPlugin var2) {
         int var3 = var1.getPreferredPosition();
         int var4 = var2.getPreferredPosition();
         return var3 > var4 ? 1 : (var3 < var4 ? -1 : 0);
      }
   };

   String getProduct();

   int getPreferredPosition();

   void initPlugin() throws Exception;

   default String forEngine() {
      return "*";
   }

   default boolean disabledForSpecialTrial() {
      return false;
   }
}
