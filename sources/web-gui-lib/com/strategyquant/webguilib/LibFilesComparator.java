package com.strategyquant.webguilib;

import java.util.Comparator;

public class LibFilesComparator implements Comparator<String> {
   private final String[] libOrders = new String[]{
      "jquery-3.1.1.min.js",
      "dhtmlx.js",
      "bootstrap.min.js",
      "angular.min.js",
      "angular-ui-router.min.js",
      "bootstrap-select.min.js",
      "bootstrap-timepicker.min.js",
      "bootstrap-datetimepicker.min.js",
      "bootstrap-datetimepicker.uk.js",
      "ui-bootstrap-tpls-1.2.5.min.js",
      "ui-bootstrap-modal-tpls-1.2.4.min.js",
      "split-pane.js",
      "angular-confirm.min.js",
      "taskmanager-breadcrumb.js",
      "ngStorage.js",
      "spin.min.js",
      "spinner.js",
      "angular-fullscreen.js",
      "howler.min.js",
      "sqnotice.js",
      "moment.min.js",
      "elessar.js",
      "elessarcomponent.js",
      "react.min.js",
      "react-dom.min.js",
      "main.min.js",
      "d3-dsv.js",
      "d3-time-format.min.js",
      "jquery.flot.min.js",
      "jquery.flot.time.js",
      "jquery.flot.tooltip.min.js",
      "jquery.range-min.js",
      "clipboard.min.js",
      "ace.js",
      "ext-language_tools.js"
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
