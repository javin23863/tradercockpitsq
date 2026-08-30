package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;

public class SwapTypes extends JSONAble {
   public static final String POINTS = "points";
   public static final String PERCENT = "percent";
   public static final String MONEY = "money";

   public static String toString(String var0) {
      switch (var0) {
         case "points":
            return L.t("points", new Object[0]);
         case "percent":
            return "%";
         case "money":
            return "$";
         default:
            return L.t("Unknown", new Object[0]);
      }
   }
}
