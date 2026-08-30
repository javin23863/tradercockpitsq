package com.strategyquant.webguilib;

import java.util.Comparator;

public class CSSFilesComparator implements Comparator<String> {
   private final String[] libOrders = new String[]{
      "elessar.css",
      "sqnoticeico.css",
      "sqnotice.css",
      "bootstrap.min.css",
      "font-awesome.min.css",
      "split-pane.css",
      "dhtmlx.css",
      "jquery.range.css",
      "font-icons.css",
      "sq4.css",
      "newdesign.css"
   };

   public int compare(String var1, String var2) {
      int var3 = this.getIndex(var1);
      int var4 = this.getIndex(var2);
      if (var3 < var4) {
         return -1;
      } else {
         return var3 > var4 ? 1 : 0;
      }
   }

   private int getIndex(String var1) {
      for (int var2 = 0; var2 < this.libOrders.length; var2++) {
         if (var1.endsWith(this.libOrders[var2])) {
            return var2;
         }
      }

      return 1000;
   }
}
