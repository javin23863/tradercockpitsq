package com.strategyquant.webguilib;

import java.util.Comparator;

public class JSFilesComparator implements Comparator<String> {
   public int compare(String var1, String var2) {
      int var3 = this.getValue(var1);
      int var4 = this.getValue(var2);
      if (var3 < var4) {
         return -1;
      } else {
         return var3 > var4 ? 1 : 0;
      }
   }

   private int getValue(String var1) {
      if (var1.contains("sq-tools")) {
         return var1.contains("sqbackend") ? 1 : 0;
      } else if (var1.endsWith("module.js")) {
         return 2;
      } else {
         return var1.endsWith("Service.js") ? 3 : 100;
      }
   }
}
